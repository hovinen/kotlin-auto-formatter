package org.kotlin.formatter.scanning.nodepattern

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.kotlin.formatter.Token
import java.util.Stack

class State(private val id: Int = idCounter++) {
    private val transitions = mutableListOf<Transition>()

    internal var action: EvaluationAction = Evaluation.consume
        private set

    internal fun addTransition(transition: Transition): State {
        transitions.add(transition)
        return this
    }

    internal val nextStates get(): Set<State> = transitions.map { it.state }.toSet()

    internal val immediateNextStates get(): List<State> =
        transitions.filterIsInstance<EpsilonTransition>().map { it.state }

    internal fun matchingNextStates(node: ASTNode): List<State> =
        transitions.filter { it is MatchingTransition && it.matcher(node) }.map { it.state }

    internal fun installAction(action: Action) {
        this.action = Evaluation.consumingAction(action)
    }

    internal fun installPushTokens() {
        action = Evaluation.tokenPushingAction
    }

    internal fun installTokenMapper(mapper: (List<Token>) -> List<Token>) {
        action = action andThen Evaluation.tokenMappingAction(mapper)
    }

    internal fun accumulate() {
        action = Evaluation.accumulate
    }

    internal fun combineWith(other: State) {
        transitions.addAll(other.transitions)
        action = action andThen other.action
    }

    val isTerminal: Boolean get() = transitions.isEmpty()

    override fun toString() = "State($id)"

    companion object {
        private var idCounter = 0
    }
}

internal typealias Action = (List<ASTNode>) -> List<Token>
internal typealias EvaluationAction = (Evaluation, ASTNode?) -> Evaluation

private infix fun EvaluationAction.andThen(other: EvaluationAction): EvaluationAction =
    when {
        this == Evaluation.consume -> other
        other == Evaluation.consume -> this
        else -> { evaluation, astNode -> other(this(evaluation, astNode), astNode) }
    }

internal data class Evaluation(
    private val nodes: List<ASTNode>,
    private val tokenStack: Stack<List<Token>>
) {
    internal val tokens: List<Token> get() = tokenStack.peek()

    companion object {
        internal val accumulate: EvaluationAction =
            { evaluation, astNode ->
                Evaluation(evaluation.nodes.plusIfNonNull(astNode), evaluation.tokenStack)
            }

        internal val consume: EvaluationAction =
            { evaluation, _ -> Evaluation(listOf(), evaluation.tokenStack) }

        internal fun consumingAction(action: Action): EvaluationAction =
            { evaluation, _ ->
                evaluation.tokenStack.push(evaluation.tokenStack.pop().plus(action(evaluation.nodes)))
                Evaluation(listOf(), evaluation.tokenStack)
            }

        val tokenPushingAction: EvaluationAction =
            { evaluation, _ ->
                evaluation.tokenStack.push(listOf())
                Evaluation(evaluation.nodes, evaluation.tokenStack)
            }

        fun tokenMappingAction(mapper: (List<Token>) -> List<Token>): EvaluationAction =
            { evaluation, _ ->
                val topTokens = evaluation.tokenStack.pop()
                evaluation.tokenStack.push(evaluation.tokenStack.pop().plus(mapper(topTokens)))
                Evaluation(evaluation.nodes, evaluation.tokenStack)
            }
    }
}

private fun <E> List<E>.plusIfNonNull(element: E?): List<E> =
    if (element == null) {
        this
    } else {
        this.plus(element)
    }

