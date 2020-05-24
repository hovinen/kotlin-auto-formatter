package org.kotlin.formatter.scanning.nodepattern

import java.util.Stack
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.kotlin.formatter.Token

/**
 * A single state in a nondeterministic finite state automaton with ε-moves.
 *
 * @property id a unique integer ID of this state; for use in debugging only
 */
internal class State(private val id: Int = idCounter++) {
    private val transitions = mutableListOf<Transition>()

    /**
     * The action to be performed when passing through this state during NFA evaluation.
     *
     * By default, the action *consumes* the input, so that it is thrown out and not stored for use
     * by later actions.
     */
    internal var action: EvaluationAction = Evaluation.consume
        private set

    /**
     * Adds the given [Transition] as an out-transition from this state.
     *
     * Returns this instance for chaining.
     */
    internal fun addTransition(transition: Transition): State {
        transitions.add(transition)
        return this
    }

    /** All [State] which result from a single out-transition from this state. */
    internal val nextStates
        get(): Set<State> = transitions.map { it.state }.toSet()

    /** All [State] which result from a single ε-transition from this state. */
    internal val immediateNextStates
        get(): List<State> = transitions.filterIsInstance<EpsilonTransition>().map { it.state }

    /**
     * All [State] which result from single transitions from this state which match the given
     * [ASTNode].
     *
     * This does not include ε-transitions.
     */
    internal fun matchingNextStates(node: ASTNode): List<State> =
        transitions.filter { it is MatchingTransition && it.matcher(node) }.map { it.state }

    /**
     * Adds the given [Action] to be executed when the NFA passes through this state.
     *
     * This overrides any existing action attached to this state.
     *
     * The action is marked *consuming*, meaning that the [ASTNode] are passed to the [Action] and
     * then discarded. They are not passed to any [Action] on any later state during evaluation.
     */
    internal fun installAction(action: Action) {
        this.action = Evaluation.consumingAction(action)
    }

    /**
     * Installs as an action a [Evaluation.tokenPushingAction].
     *
     * This overrides any existing action attached to this state.
     */
    internal fun installPushTokens() {
        action = Evaluation.tokenPushingAction
    }

    /**
     * Installs the given transform on a list of [Token] as an action on this state.
     *
     * The transform is executed on the tokens accumulated since passing through the last state with
     * a [Evaluation.tokenPushingAction], or since the first state if no such state exists. The
     * output of this mapping then replaces the accumulated tokens and are accumulated to the list
     * of tokens preceding the last [Evaluation.tokenPushingAction]. The last
     * [Evaluation.tokenPushingAction] is then removed from consideration for any future actions
     * installed by this method on any later states.
     *
     * The installed action will occur in addition to any existing action on this state.
     */
    internal fun installTokenMapper(mapper: (List<Token>) -> List<Token>) {
        action = action andThen Evaluation.tokenMappingAction(mapper)
    }

    /**
     * Installs an action to accumulate to the list of [ASTNode] to be passed to the next action the
     * [ASTNode] which was the current input when passing through this state.
     *
     * This overrides any existing action attached to this state.
     */
    internal fun accumulate() {
        action = Evaluation.accumulate
    }

    /**
     * Combines this state with the given [State].
     *
     * All out-transitions from [other] are added to this, and the [action] on [other] is performed
     * after the [action] on this instance.
     */
    internal fun combineWith(other: State) {
        transitions.addAll(other.transitions)
        action = action andThen other.action
    }

    /**
     * Whether this instance is terminal, which is the case if there are no out-transitions from it.
     */
    internal val isTerminal: Boolean
        get() = transitions.isEmpty()

    override fun toString() = "State($id)"

    companion object {
        private var idCounter = 0
    }
}

/**
 * A transformation from a list of [ASTNode] to a list of [Token].
 *
 * This is executed on [ASTNode] accumulated through a sequence of [State] through which the NFA
 * passes.
 */
internal typealias Action = (List<ASTNode>) -> List<Token>

/**
 * A single step in executing the actions associated with a given path through an NFA.
 *
 * The input [ASTNode] is the current input of the state at the given [Evaluation]. This is be null
 * if the out-transition at that [Evaluation] is an ε-transition.
 */
internal typealias EvaluationAction = (Evaluation, ASTNode?) -> Evaluation

/**
 * Combines two [EvaluationAction], producing a new [EvaluationAction] which executes the receiver
 * instance followed by [other].
 *
 * If either the receiver or [other] is [Evaluation.consume], then the respective other object is
 * returned.
 */
private infix fun EvaluationAction.andThen(other: EvaluationAction): EvaluationAction =
    when {
        this == Evaluation.consume -> other
        other == Evaluation.consume -> this
        else -> { evaluation, astNode -> other(this(evaluation, astNode), astNode) }
    }

/**
 * The result of running the cumulate actions up to a particular state in a path through an NFA.
 *
 * The property [nodes] is the accumulated sequence of [ASTNode] to be passed as input for
 * transformation into a sequence of [Token] as of this step. This accumulates input in particular
 * when the [EvaluationAction] is [Evaluation.accumulate] and is reset after either an
 * [Evaluation.consumingAction] or an [Evaluation.consume].
 *
 * The property [tokenStack] is the output of transforming [ASTNode] into [Token]. It is represented
 * as a [Stack] to facilitate transformations on lists of [Token] via [State.installTokenMapper].
 * The action [Evaluation.tokenPushingAction] pushes a new empty list to the stack, which may then
 * accumulate output [Token]. When an [Evaluation.tokenMappingAction] is encountered, it runs the
 * mapper on the top element of the stack and pops that element, accumulating the resulting elements
 * to the new top stack element.
 *
 * Evaluations are immutable. During evaluation, each step along an accepting path through the NFA
 * has an associated [EvaluationAction] which maps each [Evaluation] to its next [Evaluation].
 */
internal data class Evaluation(
    private val nodes: List<ASTNode>,
    private val tokenStack: Stack<List<Token>>
) {
    /**
     * The list of [Token] accumulated since the last [tokenPushingAction], or since the start of
     * NFA evalution if there is no previous [tokenPushingAction].
     */
    internal val tokens: List<Token>
        get() = tokenStack.peek()

    companion object {
        /** An [EvaluationAction] to accumulate the current [ASTNode] to [nodes]. */
        internal val accumulate: EvaluationAction = { evaluation, astNode ->
            Evaluation(evaluation.nodes.plusIfNonNull(astNode), evaluation.tokenStack)
        }

        /** An [EvaluationAction] to throw away all [nodes]. */
        internal val consume: EvaluationAction = { evaluation, _ ->
            Evaluation(listOf(), evaluation.tokenStack)
        }

        /**
         * An [EvaluationAction] to run the given [Action] on currently accumulated [nodes] and
         * accumulate the resulting list of [Token] to the top element of [tokenStack]. The
         * accumulated [ASTNode] are then thrown out.
         *
         * The [ASTNode] associated with the current NFA path step is not passed to the [Action],
         * but is ignored by this [EvaluationAction].
         */
        internal fun consumingAction(action: Action): EvaluationAction = { evaluation, _ ->
            evaluation.tokenStack.push(evaluation.tokenStack.pop().plus(action(evaluation.nodes)))
            Evaluation(listOf(), evaluation.tokenStack)
        }

        /** An [EvaluationAction] to push a new empty list to [tokenStack]. */
        internal val tokenPushingAction: EvaluationAction = { evaluation, _ ->
            evaluation.tokenStack.push(listOf())
            Evaluation(evaluation.nodes, evaluation.tokenStack)
        }

        /**
         * An [EvaluationAction] to pop the top of [tokenStack], run the given [mapper] on that
         * list of [Token], and accumulate the resulting list of [Token] to the new top [tokenStack]
         * element.
         */
        internal fun tokenMappingAction(mapper: (List<Token>) -> List<Token>): EvaluationAction =
            { evaluation, _ ->
                val topTokens = evaluation.tokenStack.pop()
                evaluation.tokenStack.push(evaluation.tokenStack.pop().plus(mapper(topTokens)))
                Evaluation(evaluation.nodes, evaluation.tokenStack)
            }
    }
}

/**
 * Returns a new list with the receiver plus [element] if the latter is not null; otherwise returns
 * the receiver unchanged.
 */
private fun <E> List<E>.plusIfNonNull(element: E?): List<E> =
    if (element == null) {
        this
    } else {
        this.plus(element)
    }
