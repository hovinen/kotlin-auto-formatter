package org.kotlin.formatter.scanning.nodepattern

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.kotlin.formatter.Token
import java.util.Stack

/**
 * A domain-specific language for building [NodePattern].
 *
 * The DSL methods consist of the following:
 *
 *   * *Matchers*, which match either single [ASTNode] or subsequences thereof.
 *   * *Actions*, which transform an accumulated sequence of [ASTNode] into a list of [Token].
 *   * *Token mappers*, which apply additional transformations to the output [Token].
 *
 * The algorithm is based on
 * [Thompson's construction](https://en.wikipedia.org/wiki/Thompson%27s_construction) for NFAs from
 * regular expressions.
 */
class NodePatternBuilder {
    private val elementStack = Stack<Element>()

    /**
     * Matches any [ASTNode].
     */
    fun anyNode(): NodePatternBuilder = nodeMatching { true }

    /**
     * Matches a [ASTNode] whose [ASTNode.getElementType] is the given [type].
     */
    fun nodeOfType(type: IElementType): NodePatternBuilder = nodeMatching { it.elementType == type }

    /**
     * Matches a [ASTNode] whose [ASTNode.getElementType] is any of the given [type].
     */
    fun nodeOfOneOfTypes(vararg types: IElementType): NodePatternBuilder {
        val typeSet = setOf(*types)
        return nodeMatching { typeSet.contains(it.elementType) }
    }

    /**
     * Matches any whitespace [ASTNode].
     */
    fun whitespace(): NodePatternBuilder = nodeOfType(KtTokens.WHITE_SPACE)

    /**
     * Matches a whitespace [ASTNode] only if it contains a newline character `\n`.
     */
    fun whitespaceWithNewline(): NodePatternBuilder = nodeMatching {
        it.elementType == KtTokens.WHITE_SPACE && it.textContains('\n')
    }

    /**
     * Matches a possibly empty sequence of whitespace [ASTNode].
     */
    fun possibleWhitespace() = zeroOrMore { whitespace() }

    /**
     * Matches the end of input.
     *
     * All constructs must end with this matcher.
     */
    fun end(): NodePatternBuilder = nodeMatching { it == TerminalNode }

    private fun nodeMatching(matcher: (ASTNode) -> Boolean): NodePatternBuilder {
        val finalState = terminalState()
        elementStack.push(
            Element(
                State().addTransition(MatchingTransition(matcher, finalState)),
                finalState
            )
        )
        return this
    }

    /**
     * Matches either the sequence of [ASTNode] specified by the given [init] or the sequence
     * specified by the parameter to [EitherOrBuilder.or].
     */
    fun either(init: NodePatternBuilder.() -> Unit) = EitherOrBuilder(init)

    /**
     * Represents an either-or expression.
     */
    inner class EitherOrBuilder(private val eitherInit: NodePatternBuilder.() -> Unit) {
        /**
         * Specifies a second sequence of [ASTNode] which this either-or expression matches.
         *
         * The first expression is given by the argument to [NodePatternBuilder.either].
         */
        infix fun or(orInit: NodePatternBuilder.() -> Unit): NodePatternBuilder {
            val eitherElement = buildSubgraph(eitherInit)
            val orElement = buildSubgraph(orInit)
            val initialState =
                State()
                    .addTransition(EpsilonTransition(eitherElement.initialState))
                    .addTransition(EpsilonTransition(orElement.initialState))
            val finalState = terminalState()
            eitherElement.finalState.addTransition(EpsilonTransition(finalState))
            orElement.finalState.addTransition(EpsilonTransition(finalState))
            elementStack.push(Element(initialState, finalState))
            return this@NodePatternBuilder
        }
    }

    /**
     * Matches the sequence specified by [init] optionally or its complete absence.
     */
    fun zeroOrOne(init: NodePatternBuilder.() -> Unit): NodePatternBuilder {
        val subgraphElement = buildSubgraph(init)
        val finalState = terminalState()
        val initialState =
            State()
                .addTransition(EpsilonTransition(subgraphElement.initialState))
                .addTransition(EpsilonTransition(finalState))
        subgraphElement.finalState.addTransition(EpsilonTransition(finalState))
        elementStack.push(Element(initialState, finalState))
        return this
    }

    /**
     * Matches any number of [ASTNode] in the sequence specified by [init], including none.
     *
     * This variant has greedy behaviour; see [zeroOrMoreGreedy].
     */
    fun zeroOrMore(init: NodePatternBuilder.() -> Unit): NodePatternBuilder {
        return zeroOrMoreGreedy(init)
    }

    /**
     * Matches greedily any number of [ASTNode] in the sequence specified by [init], including none.
     *
     * This method produces an NFA which prefers to match the most matching [ASTNode] sequences. For
     * example, consider the following sequence:
     *
     * ```
     * zeroOrMoreGreedy { anyNode() }
     * possibleWhitespace()
     * ```
     *
     * An input sequence with whitespace at the end may be matched either via `anyNode()` in the
     * `zeroOrMoreGreedy` block or by `possibleWhitesapce()`. The "greedy" in `zeroOrMoreGreedy`
     * prefers the former behaviour, so that the whitespace nodes are attached to `zeroOrMoreGreedy`
     * and not `possibleWhitespace`. In particular, any [actions][andThen] attached to
     * `zeroOrMoreGreedy` see the following whitespace nodes.
     */
    fun zeroOrMoreGreedy(init: NodePatternBuilder.() -> Unit): NodePatternBuilder {
        val subgraphElement = buildSubgraph(init)
        val finalState = terminalState()
        subgraphElement.finalState
            .addTransition(EpsilonTransition(subgraphElement.initialState))
            .addTransition(EpsilonTransition(finalState))
        elementStack.push(Element(subgraphElement.finalState, finalState))
        return this
    }

    /**
     * Matches frugally any number of [ASTNode] in the sequence specified by [init], including none.
     *
     * This method produces an NFA which prefers to match the fewest matching [ASTNode] sequences.
     * For example, consider the following sequence:
     *
     * ```
     * zeroOrMoreFrugal { anyNode() }
     * possibleWhitespace()
     * ```
     *
     * An input sequence with whitespace at the end may be matched either via `anyNode()` in the
     * `zeroOrMoreGreedy` block or by `possibleWhitesapce()`. The "frugal" in `zeroOrMoreGreedy`
     * prefers the latter behaviour, so that the whitespace nodes are attached to
     * `possibleWhitespace` and not `zeroOrMoreFrugal`. In particular, any [actions](andThen)
     * attached to `zeroOrMoreFrugal` do not see the following whitespace nodes.
     */
    fun zeroOrMoreFrugal(init: NodePatternBuilder.() -> Unit): NodePatternBuilder {
        val subgraphElement = buildSubgraph(init)
        val finalState = terminalState()
        subgraphElement.finalState
            .addTransition(EpsilonTransition(finalState))
            .addTransition(EpsilonTransition(subgraphElement.initialState))
        elementStack.push(Element(subgraphElement.finalState, finalState))
        return this
    }

    /**
     * Matches exactly one of the sequence of [ASTNode] specified by [init].
     *
     * This may be used to accumulate a sequence of [ASTNode] which is to appear exactly once and
     * perform an action on the full accumulated sequence.
     */
    fun exactlyOne(init: NodePatternBuilder.() -> Unit): NodePatternBuilder {
        elementStack.push(buildSubgraph(init))
        return this
    }

    /**
     * Matches one or more sequences of [ASTNode] specified by [init].
     *
     * This variant has greedy behaviour; see [zeroOrMoreGreedy].
     */
    fun oneOrMore(init: NodePatternBuilder.() -> Unit): NodePatternBuilder {
        return oneOrMoreGreedy(init)
    }

    /**
     * Matches greedily one or more sequences of [ASTNode] specified by [init].
     *
     * For a description of greedy matching, see [zeroOrMoreGreedy].
     */
    fun oneOrMoreGreedy(init: NodePatternBuilder.() -> Unit): NodePatternBuilder {
        val subgraphElement = buildSubgraph(init)
        val finalState = terminalState()
        subgraphElement.finalState
            .addTransition(EpsilonTransition(subgraphElement.initialState))
            .addTransition(EpsilonTransition(finalState))
        elementStack.push(Element(subgraphElement.initialState, finalState))
        return this
    }

    /**
     * Matches frugally one or more sequences of [ASTNode] specified by [init].
     *
     * For a description of frugal matching, see [zeroOrMoreFrugal].
     */
    fun oneOrMoreFrugal(init: NodePatternBuilder.() -> Unit): NodePatternBuilder {
        val subgraphElement = buildSubgraph(init)
        val finalState = terminalState()
        subgraphElement.finalState
            .addTransition(EpsilonTransition(finalState))
            .addTransition(EpsilonTransition(subgraphElement.initialState))
        elementStack.push(Element(subgraphElement.initialState, finalState))
        return this
    }

    private fun buildSubgraph(init: NodePatternBuilder.() -> Unit): Element {
        val builder = NodePatternBuilder()
        init(builder)
        builder.reduce()
        return builder.elementStack.pop()
    }

    /**
     * Specifies the given [Action] to be performed on the sequence of [ASTNode] accumulated by the
     * immediately preceding [ASTNode] matcher.
     */
    infix fun andThen(action: Action) {
        val topElement = elementStack.peek()
        val coveredStates = mutableSetOf<State>()
        var states = setOf(topElement.initialState)
        while (states.isNotEmpty()) {
            val nextStates = mutableSetOf<State>()
            states.forEach {
                it.accumulate()
                coveredStates.add(it)
                nextStates.addAll(it.nextStates.minus(coveredStates))
            }
            states = nextStates
        }
        topElement.finalState.installAction(action)
    }

    /**
     * Specifies that the output [Token] accumulated by the immediately preceding [ASTNode] matcher
     * are to be transformed by the given [tokenMapper] before being output.
     */
    infix fun thenMapTokens(tokenMapper: (List<Token>) -> List<Token>) {
        val topElement = elementStack.pop()
        val initialState =
            State().addTransition(EpsilonTransition(topElement.initialState))
        initialState.installPushTokens()
        topElement.finalState.installTokenMapper(tokenMapper)
        elementStack.push(Element(initialState, topElement.finalState))
    }

    /**
     * Constructs a [NodePattern] from this builder.
     */
    internal fun build(): NodePattern {
        reduce()
        return NodePattern(elementStack.pop().initialState)
    }

    private fun reduce() {
        while (elementStack.size > 1) {
            concatenate()
        }
    }

    private fun concatenate() {
        val secondElement = elementStack.pop()
        val firstElement = elementStack.pop()
        firstElement.finalState.combineWith(secondElement.initialState)
        elementStack.push(Element(firstElement.initialState, secondElement.finalState))
    }

    private fun terminalState() = State()
}

/**
 * Constructs a [NodePattern] by applying the instructions in [init] to a [NodePatternBuilder].
 *
 * The input specifies a sequence of *matchers* of [ASTNode], possibly grouped into subsequences. It
 * is analogous to a regular expression for strings. To specify the sequence, invoke the matcher
 * methods in [NodePatternBuilder].
 *
 * For example, the following node pattern matches a comma-separated list of identifiers:
 *
 * ```
 * nodePattern {
 *     possibleWhitespace()
 *     oneOrMore {
 *         nodeOfType(KtTokens.IDENTIFIER)
 *         nodeOfType(KtTokens.COMMA)
 *         possibleWhitespace()
 *     }
 *     end()
 * }
 * ```
 *
 * To transform the [ASTNode] into [Token], apply the method [andThen] after a block which specifies
 * the nodes to be transformed:
 *
 * ```
 * nodePattern {
 *     possibleWhitespace()
 *     oneOrMore {
 *         nodeOfType(KtTokens.IDENTIFIER) andThen { nodes ->
 *             listOf(
 *                 LeafNodeToken(nodes[0].text),
 *                 LeafNodeToken(","),
 *                 WhitespaceToken(content = " ")
 *             )
 *         }
 *         nodeOfType(KtTokens.COMMA)
 *         possibleWhitespace()
 *     }
 *     end()
 * }
 * ```
 *
 * A given sequence of [ASTNode] may only be transformed into [Token] once, after which the nodes
 * are discarded and no longer available for further transformations. For example, the following
 * does not work as intended.
 *
 * ```
 * nodePattern {
 *     possibleWhitespace()
 *     oneOrMore {
 *         nodeOfType(KtTokens.IDENTIFIER) andThen { nodes -> listOf(...) }
 *         nodeOfType(KtTokens.COMMA)
 *         possibleWhitespace()
 *     } andThen { allNodes -> ...work with nodes... } // WRONG: identifiers were already consumed
 *     end()
 * }
 * ```
 *
 * The sequence must end with a call to [NodePatternBuilder.end].
 */
fun nodePattern(init: NodePatternBuilder.() -> Unit): NodePattern =
    NodePatternBuilder().apply(init).build()

/**
 * A subgraph of an NFA corresponding to a subsequence of [ASTNode] to be matched.
 */
private class Element internal constructor(
    internal val initialState: State,
    internal val finalState: State
)
