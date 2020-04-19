package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.ClosingForcedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token

internal class BlockScanner(private val kotlinScanner: KotlinScanner) {
    fun scanBlock(node: ASTNode): List<Token> {
        val children = node.children().toList()
        val indexOfLBrace = children.indexOfFirst { it.elementType == KtTokens.LBRACE }
        val indexOfRBrace = children.indexOfLast { it.elementType == KtTokens.RBRACE }
        return if (indexOfLBrace != -1 && indexOfRBrace != -1) {
            val innerTokens = kotlinScanner.scanNodes(children.subList(indexOfLBrace + 1, indexOfRBrace), KotlinScanner.ScannerState.BLOCK)
            val innerTokensWithClosingBreakToken =
                if (innerTokens.isNotEmpty() && innerTokens.last() is ForcedBreakToken) {
                    listOf(
                        *innerTokens.subList(0, innerTokens.size - 1).toTypedArray(),
                        ClosingForcedBreakToken
                    )
                } else {
                    innerTokens
                }
            listOf(
                LeafNodeToken("{"),
                BeginToken(length = lengthOfTokens(innerTokens), state = State.CODE),
                *innerTokensWithClosingBreakToken.toTypedArray(),
                EndToken,
                LeafNodeToken("}")
            )
        } else {
            val tokens = kotlinScanner.scanNodes(children, KotlinScanner.ScannerState.BLOCK)
            replaceTerminalForcedBreakTokenWithClosingForcedBreakToken(tokens)
        }
    }

    private fun replaceTerminalForcedBreakTokenWithClosingForcedBreakToken(
        tokens: List<Token>
    ): List<Token> {
        var index = tokens.size - 1
        while (index > 0 && tokens[index] is EndToken) {
            index--
        }
        return if (index > 0 && tokens[index - 1] is ForcedBreakToken) {
            listOf(
                *tokens.subList(0, index - 1).toTypedArray(),
                ClosingForcedBreakToken,
                *tokens.subList(index, tokens.size).toTypedArray()
            )
        } else {
            tokens
        }
    }
}
