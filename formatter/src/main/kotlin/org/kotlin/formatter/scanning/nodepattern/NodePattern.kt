package org.kotlin.formatter.scanning.nodepattern

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.kotlin.formatter.Token
import org.kotlin.formatter.scanning.tail

class NodePattern internal constructor(private val initialState: State) {
    fun matchSequence(nodes: Iterable<ASTNode>): List<Token> {
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
                    result.addAll(transition.action(accumulatedNodes, currentNode))
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
}

internal object TerminalNode: LeafPsiElement(KtTokens.WHITE_SPACE, "")
