package org.kotlin.formatter.scanning.nodepattern

import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * A transition with target state [state] in an NFA.
 */
internal sealed class Transition(internal val state: State)

/**
 * A transition which may only be applied if [matcher] evaluates to `true` on the current input
 * [ASTNode] during NFA evaluation.
 *
 * The transition consumes the matched [ASTNode] so that it cannot be used in later
 * [MatchingTransition].
 */
internal class MatchingTransition(
    internal val matcher: (ASTNode) -> Boolean,
    state: State
) : Transition(state)

/**
 * An ε-transition in an NFA.
 *
 * Such a transition applies immediately and unconditionally as soon as its origin state is reached.
 */
internal class EpsilonTransition(state: State) : Transition(state)
