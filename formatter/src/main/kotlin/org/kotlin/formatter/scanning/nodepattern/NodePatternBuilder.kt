package org.kotlin.formatter.scanning.nodepattern

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import java.util.Stack

class NodePatternBuilder {
    private val elementStack = Stack<Element>()

    fun anyNode(): NodePatternBuilder = nodeMatching { true }

    fun nodeOfType(type: IElementType): NodePatternBuilder = nodeMatching { it.elementType == type }

    fun nodeOfOneOfTypes(vararg types: IElementType): NodePatternBuilder {
        val typeSet = setOf(*types)
        return nodeMatching { typeSet.contains(it.elementType) }
    }

    fun whitespace(): NodePatternBuilder = nodeOfType(KtTokens.WHITE_SPACE)

    fun possibleWhitespace() = zeroOrMore { whitespace() }

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

    fun zeroOrMore(init: NodePatternBuilder.() -> Unit): NodePatternBuilder {
        return zeroOrMoreGreedy(init)
    }

    fun zeroOrMoreGreedy(init: NodePatternBuilder.() -> Unit): NodePatternBuilder {
        val subgraphElement = buildSubgraph(init)
        val finalState = terminalState()
        subgraphElement.finalState
            .addTransition(EpsilonTransition(subgraphElement.initialState))
            .addTransition(EpsilonTransition(finalState))
        elementStack.push(Element(subgraphElement.finalState, finalState))
        return this
    }

    fun zeroOrMoreFrugal(init: NodePatternBuilder.() -> Unit): NodePatternBuilder {
        val subgraphElement = buildSubgraph(init)
        val finalState = terminalState()
        subgraphElement.finalState
            .addTransition(EpsilonTransition(finalState))
            .addTransition(EpsilonTransition(subgraphElement.initialState))
        elementStack.push(Element(subgraphElement.finalState, finalState))
        return this
    }

    fun exactlyOne(init: NodePatternBuilder.() -> Unit): NodePatternBuilder {
        elementStack.push(buildSubgraph(init))
        return this
    }

    fun oneOrMore(init: NodePatternBuilder.() -> Unit): NodePatternBuilder {
        return oneOrMoreGreedy(init)
    }

    fun oneOrMoreGreedy(init: NodePatternBuilder.() -> Unit): NodePatternBuilder {
        val subgraphElement = buildSubgraph(init)
        val finalState = terminalState()
        subgraphElement.finalState
            .addTransition(EpsilonTransition(subgraphElement.initialState))
            .addTransition(EpsilonTransition(finalState))
        elementStack.push(Element(subgraphElement.initialState, finalState))
        return this
    }

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

fun nodePattern(init: NodePatternBuilder.() -> Unit): NodePattern =
    NodePatternBuilder().apply(init).build()

private class Element internal constructor(
    internal val initialState: State,
    internal val finalState: State
)
