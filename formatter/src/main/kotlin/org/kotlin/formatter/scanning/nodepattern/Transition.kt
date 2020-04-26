package org.kotlin.formatter.scanning.nodepattern

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.kotlin.formatter.Token

internal sealed class Transition(internal val state: State) {
    abstract fun matches(node: ASTNode): Boolean
}

internal class ConsumingTransition(
    private val matcher: (ASTNode) -> Boolean,
    state: State,
    internal val action: Action
) : Transition(state) {
    override fun matches(node: ASTNode) = matcher(node)
}

internal class AccumulatingTransition(state: State) : Transition(state) {
    override fun matches(node: ASTNode) = true
}

internal typealias Action = (List<ASTNode>, ASTNode) -> List<Token>

internal fun emptyAction(): Action = { _, _ -> listOf() }
