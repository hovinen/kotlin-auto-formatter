package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.ClosingForcedBreakToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token

internal class WhenForExpressionScanner(private val kotlinScanner: KotlinScanner): NodeScanner {
    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> {
        val childNodes = node.children().toList()
        val indexOfLeftParenthesis = childNodes.indexOfFirst { it.elementType == KtTokens.LPAR }
        val nodesUntilLeftParenthesis = childNodes.subList(0, indexOfLeftParenthesis)
        val tokensUntilLeftParenthesis = kotlinScanner.scanNodes(nodesUntilLeftParenthesis, ScannerState.STATEMENT)
        val indexOfRightParenthesis = childNodes.indexOfFirst { it.elementType == KtTokens.RPAR }
        val nodesBetweenParentheses = childNodes.subList(indexOfLeftParenthesis + 1, indexOfRightParenthesis)
        val tokensBetweenParentheses = kotlinScanner.scanNodes(nodesBetweenParentheses, ScannerState.STATEMENT)
        val nodesAfterRightParenthesis = childNodes.subList(indexOfRightParenthesis + 1, childNodes.size)
        val tokensAfterRightParenthesis = kotlinScanner.scanNodes(nodesAfterRightParenthesis, ScannerState.BLOCK)
        val innerTokens = listOf(
            *tokensUntilLeftParenthesis.toTypedArray(),
            LeafNodeToken("("),
            BeginToken(length = lengthOfTokens(tokensBetweenParentheses), state = State.CODE),
            *tokensBetweenParentheses.toTypedArray(),
            ClosingSynchronizedBreakToken(whitespaceLength = 0),
            EndToken,
            LeafNodeToken(")"),
            *tokensAfterRightParenthesis.toTypedArray()
        )
        return inBeginEndBlock(innerTokens, State.CODE)
    }
    
    fun scanWhenExpression(
        node: ASTNode,
        scannerState: ScannerState
    ): List<Token> {
        val innerTokens = scan(node, scannerState)
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
