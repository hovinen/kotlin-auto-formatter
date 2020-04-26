package org.kotlin.formatter.scanning.nodepattern

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.kotlin.formatter.Token

class State {
    private val transitions = mutableListOf<Transition>()

    internal var action: EvaluationAction = Evaluation.consume
        private set

    internal fun addTransition(transition: Transition): State {
        transitions.add(transition)
        return this
    }

    internal val nextStates get(): Set<State> = transitions.map { it.state }.toSet()

    internal val immediateNextStates get(): Set<State> =
        transitions.filterIsInstance<EpsilonTransition>().map { it.state }.toSet()

    internal fun matchingNextStates(node: ASTNode): List<State> =
        transitions.filter { it is MatchingTransition && it.matcher(node) }.map { it.state }

    infix fun installAction(action: Action) {
        this.action = Evaluation.consumingAction(action)
    }

    internal fun accumulate() {
        this.action = Evaluation.accumulate
    }

    internal fun combineWith(other: State) {
        transitions.addAll(other.transitions)
        action = action andThen other.action
    }

    val isTerminal: Boolean get() = transitions.isEmpty()
}

internal typealias Action = (List<ASTNode>) -> List<Token>
internal typealias EvaluationAction = (Evaluation, ASTNode?) -> Evaluation

private infix fun EvaluationAction.andThen(other: EvaluationAction): EvaluationAction =
    when {
        this == Evaluation.consume -> other
        other == Evaluation.consume -> this
        else -> { evaluation, astNode -> other(this(evaluation, astNode), astNode) }
    }

internal data class Evaluation(private val nodes: List<ASTNode>, internal val tokens: List<Token>) {
    companion object {
        internal val accumulate: EvaluationAction =
            { evaluation, astNode ->
                Evaluation(evaluation.nodes.plusIfNonNull(astNode), evaluation.tokens)
            }

        internal val consume: EvaluationAction =
            { evaluation, _ -> Evaluation(listOf(), evaluation.tokens) }

        internal fun consumingAction(action: Action): EvaluationAction =
            { evaluation, _ ->
                Evaluation(listOf(), evaluation.tokens.plus(action(evaluation.nodes)))
            }
    }
}

private fun <E> List<E>.plusIfNonNull(element: E?): List<E> =
    if (element == null) {
        this
    } else {
        this.plus(element)
    }

