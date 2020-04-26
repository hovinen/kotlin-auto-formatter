package org.kotlin.formatter.scanning.nodepattern

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.kotlin.formatter.scanning.tail

class NodePatternBuilder {
    private val elements = mutableListOf<Element>()

    fun accumulateUntilNodeMatching(matcher: (ASTNode) -> Boolean, action: Action) {
        elements.add(MatchingElement(matcher, action))
    }

    fun skipNodesMatching(matcher: (ASTNode) -> Boolean) {
        elements.add(SkippingElement(matcher))
    }

    fun accumulateUntilEnd(action: Action) {
        elements.add(EndingElement(action))
    }

    internal fun build(): NodePattern = NodePattern(buildStateMachine(elements))
}

fun nodePattern(init: NodePatternBuilder.() -> Unit): NodePattern =
    NodePatternBuilder().apply(init).build()

private sealed class Element(internal val action: Action)

private class MatchingElement(internal val matcher: (ASTNode) -> Boolean, action: Action)
    : Element(action)

private class SkippingElement(internal val matcher: (ASTNode) -> Boolean)
    : Element(emptyAction())

private class EndingElement(action: Action) : Element(action)

private fun buildStateMachine(elements: List<Element>): State {
    if (elements.isEmpty()) {
        return TerminalState
    } else {
        val nextState = buildStateMachine(elements.tail())
        return when (val element = elements.first()) {
            is MatchingElement -> {
                val state = NonTerminalState()
                state.addTransition(ConsumingTransition(element.matcher, nextState, element.action))
                    .addTransition(AccumulatingTransition(state))
            }
            is SkippingElement -> {
                val state = NonTerminalState()
                state.addTransition(ConsumingTransition(element.matcher, state, emptyAction()))
                    .addTransition(AccumulatingTransition(nextState))
            }
            is EndingElement -> {
                val state = NonTerminalState()
                state
                    .addTransition(
                        ConsumingTransition({ it is TerminalNode }, nextState, element.action)
                    )
                    .addTransition(AccumulatingTransition(state))
            }
        }
    }
}
