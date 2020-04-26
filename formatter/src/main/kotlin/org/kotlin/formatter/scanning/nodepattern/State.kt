package org.kotlin.formatter.scanning.nodepattern

import org.jetbrains.kotlin.com.intellij.lang.ASTNode

internal sealed class State {
    abstract fun transition(node: ASTNode): Transition
}

internal class NonTerminalState : State() {
    private val transitions = mutableListOf<Transition>()

    fun addTransition(transition: Transition): NonTerminalState {
        transitions.add(transition)
        return this
    }

    override fun transition(node: ASTNode): Transition {
        for (transition in transitions) {
            if (transition.matches(node)) {
                return transition
            }
        }
        throw Exception("Node $node not accepted by state machine")
    }
}

internal object TerminalState : State() {
    override fun transition(node: ASTNode): Transition = AccumulatingTransition(this)
}
