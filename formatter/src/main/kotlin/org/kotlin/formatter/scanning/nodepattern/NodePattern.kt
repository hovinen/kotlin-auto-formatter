package org.kotlin.formatter.scanning.nodepattern

import java.util.Stack
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.kotlin.formatter.Token

/**
 * A nondeterministic finite state automaton with ε-moves acting on a stream of [ASTNode].
 *
 * This can be created via [NodePatternBuilder], which defines a DSL representing sequences of
 * [ASTNode] at a particular level of the Kotlin abstract syntax tree.
 *
 * When it matches the input, the NFA produces a list of [Token] as output based on the commands
 * given in the [NodePatternBuilder]. The entire input must be matched in order that the machine
 * accept.
 *
 * This implementation maintains a set of possible matching paths from the initial state, advancing
 * each candidate path first via all possible ε-moves and then by matching the next input token. The
 * machine accepts the input sequence when at least one path ends in an accepting state (i.e., one
 * with no out-transitions) and the input is exhausted. In that case, the first matching path is
 * selected and the output-producing commands along the path are executed in order.
 */
class NodePattern internal constructor(private val initialState: State) {
    /**
     * Matches the given sequence of [ASTNode] with this machine, executing the resulting commands
     * to produce a list of [Token].
     *
     * Throws an exception if the input sequence is not accepted by the machine.
     */
    fun matchSequence(nodes: Iterable<ASTNode>): List<Token> {
        var paths = listOf<PathStep>(InitialPathStep(initialState))
        for (node in nodes) {
            val states = paths.filterIsInstance<PathStepOnState>().map { it.state }.toMutableSet()
            paths = epsilonStep(paths, states)
            paths = setNodeOnPaths(paths, node)
            paths = step(paths, node)
        }
        val states = paths.filterIsInstance<PathStepOnState>().map { it.state }.toMutableSet()
        paths = epsilonStep(paths, states)
        paths = step(paths, TerminalNode)
        return paths.firstOrNull { it is FinalPathStep }?.runActions()?.tokens
            ?: throw NodeSequenceNotMatchedException(nodes)
    }

    private fun setNodeOnPaths(paths: List<PathStep>, node: ASTNode): List<PathStep> =
        paths.map { it.withNode(node) }

    private fun epsilonStep(paths: List<PathStep>, states: MutableSet<State>): List<PathStep> =
        paths.flatMap { if (it is PathStepOnState) epsilonStepForPath(it, states) else listOf(it) }

    private fun epsilonStepForPath(startingPath: PathStepOnState, visitedStates: MutableSet<State>):
        List<PathStep> {
            val newStates = startingPath.state.immediateNextStates.minus(visitedStates)
            return if (newStates.isNotEmpty()) {
                visitedStates.addAll(newStates)
                newStates.flatMap { state ->
                    epsilonStepForPath(ContinuingPathStep(state, startingPath), visitedStates)
                }
            } else {
                listOf(startingPath)
            }
        }

    private fun step(paths: List<PathStep>, node: ASTNode): List<PathStep> =
        paths.flatMap { path ->
            if (path is PathStepOnState) {
                path.state
                    .matchingNextStates(node)
                    .map { state ->
                        if (state.isTerminal) {
                            FinalPathStep(state, path)
                        } else {
                            ContinuingPathStep(state, path)
                        }
                    }
            } else {
                listOf()
            }
        }
}

/**
 * A single step on a candidate path from the initial state to the current state in the NFA.
 *
 * Paths are constructed as a linked list whose initial element is the *final* step of the path; see
 * in particular [ContinuingPathStep] and [FinalPathStep].
 */
private sealed class PathStep {
    /**
     * Run the actions associated with the NFA states of all steps up to and including this one.
     *
     * Returns the resulting [Evaluation].
     */
    abstract fun runActions(): Evaluation

    /** Creates a new [PathStep] based on this instance with the given [ASTNode] attached. */
    abstract fun withNode(node: ASTNode): PathStep
}

/**
 * A [PathStep] which has an associated [State] in the NFA.
 *
 * @property state the NFA state associated with this step
 * @property node the [ASTNode] which was used to find transitions out of this step. This is null if
 *     no input was used to find transitions, either because the out-transitions have not been
 *     determined yet or because only ε-transitions have been made. On any given path, a given
 *     [ASTNode] may only appear on one [PathStep].
 */
private abstract class PathStepOnState(internal val state: State, internal val node: ASTNode?) :
    PathStep()

/**
 * A [PathStep] located in the initial state of the NFA.
 *
 * All paths begin with an [InitialPathStep].
 */
private class InitialPathStep(state: State, node: ASTNode? = null) : PathStepOnState(state, node) {
    override fun runActions(): Evaluation =
        state.action(Evaluation(listOf(), Stack<List<Token>>().apply { push(listOf()) }), node)

    override fun withNode(node: ASTNode): PathStep = InitialPathStep(state, node)
}

/**
 * A [PathStep] representing an intermediate state, of the NFA, neither initial nor accepting.
 *
 * @property previous the previous [PathStep] of this path
 */
private class ContinuingPathStep(
    state: State,
    internal val previous: PathStep,
    node: ASTNode? = null
) : PathStepOnState(state, node) {
    override fun runActions(): Evaluation = state.action(previous.runActions(), node)

    override fun withNode(node: ASTNode): PathStep = ContinuingPathStep(state, previous, node)
}

/**
 * A [PathStep] representing the final step of the path.
 *
 * @property state the accepting state associated with this step
 * @property previous the previous [PathStep] of this path
 */
private class FinalPathStep(internal val state: State, internal val previous: PathStep) :
    PathStep() {

    override fun runActions(): Evaluation = previous.runActions()

    override fun withNode(node: ASTNode): PathStep = this
}

/** An [ASTNode] representing the end of input. */
internal object TerminalNode : LeafPsiElement(KtTokens.EOF, "")
