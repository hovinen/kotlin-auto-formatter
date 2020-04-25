package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.kotlin.formatter.Token

internal class NodePattern private constructor(private val initialState: State) {
    internal fun matchSequence(nodes: Iterable<ASTNode>): List<Token> {
        var state = initialState
        val result = mutableListOf<Token>()
        val currentNodeIterator = nodes.iterator()
        val accumulatedNodes = mutableListOf<ASTNode>()
        while (!(state is TerminalState)) {
            val currentNode =
                if (currentNodeIterator.hasNext()) currentNodeIterator.next() else TerminalNode
            val transition = state.transition(currentNode)
            when (transition) {
                is ConsumingTransition -> {
                    result.addAll(transition.action(accumulatedNodes))
                    accumulatedNodes.clear()
                }
                is AccumulatingTransition -> {
                    accumulatedNodes.add(currentNode)
                }
            }
            state = transition.state
        }
        return result
    }

    internal class Builder {
        private val elements = mutableListOf<Element>()

        fun matchingElement(matcher: (ASTNode) -> Boolean, action: (List<ASTNode>) -> List<Token>) {
            elements.add(MatchingElement(matcher, action))
        }
        
        fun endingElement(action: (List<ASTNode>) -> List<Token>) {
            elements.add(EndingElement(action))
        }

        internal fun build(): NodePattern = NodePattern(buildStateMachine(elements))
    }
}

internal fun nodePattern(init: NodePattern.Builder.() -> Unit): NodePattern =
    NodePattern.Builder().apply(init).build()

private typealias Action = (List<ASTNode>) -> List<Token>

private sealed class State {
    abstract fun transition(node: ASTNode): Transition
}

private class NonTerminalState(
    private val transitions: List<Transition>
) : State() {
    override fun transition(node: ASTNode): Transition {
        for (transition in transitions) {
            if (transition.matches(node)) {
                return transition
            }
        }
        return AccumulatingTransition(this)
    }
}

private object TerminalState : State() {
    override fun transition(node: ASTNode): Transition = AccumulatingTransition(this)
}

private sealed class Transition(internal val state: State) {
    abstract fun matches(node: ASTNode): Boolean
}

private class ConsumingTransition(
    private val matcher: (ASTNode) -> Boolean,
    state: State,
    internal val action: Action
) : Transition(state) {
    override fun matches(node: ASTNode) = matcher(node)
}

private class AccumulatingTransition(state: State) : Transition(state) {
    override fun matches(node: ASTNode) = true
}

private object TerminalNode: LeafPsiElement(KtTokens.WHITE_SPACE, "")

private sealed class Element(internal val action: Action)

private class MatchingElement(internal val matcher: (ASTNode) -> Boolean, action: Action)
    : Element(action)

private class EndingElement(action: Action) : Element(action)

private fun buildStateMachine(elements: List<Element>): State {
    if (elements.isEmpty()) {
        return TerminalState
    } else {
        val nextState = buildStateMachine(elements.tail())
        return when (val element = elements.first()) {
            is MatchingElement -> {
                NonTerminalState(
                    listOf(ConsumingTransition(element.matcher, nextState, element.action))
                )
            }
            is EndingElement -> {
                NonTerminalState(
                    listOf(ConsumingTransition({ it is TerminalNode }, nextState, element.action))
                )
            }
        }
    }
}
