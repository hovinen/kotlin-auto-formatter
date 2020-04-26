package org.kotlin.formatter.scanning.nodepattern

import org.jetbrains.kotlin.com.intellij.lang.ASTNode

internal sealed class Transition(internal val state: State)

internal class MatchingTransition(
    internal val matcher: (ASTNode) -> Boolean,
    state: State
) : Transition(state)

internal class EpsilonTransition(state: State) : Transition(state)
