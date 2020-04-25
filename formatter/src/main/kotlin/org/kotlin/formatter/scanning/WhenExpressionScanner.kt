package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.kotlin.formatter.ClosingForcedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token

internal class WhenExpressionScanner(private val whenForExpressionScanner: WhenForExpressionScanner): NodeScanner {
    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> {
        val innerTokens = whenForExpressionScanner.scan(node, scannerState)
        val tokens = inBeginEndBlock(innerTokens, State.CODE)
        return replaceTerminalForcedBreakTokenWithClosingForcedBreakToken(tokens)
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