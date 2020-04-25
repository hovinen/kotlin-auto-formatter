package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.nonBreakingSpaceToken

internal class BinaryExpressionScanner(private val kotlinScanner: KotlinScanner): NodeScanner {
    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> {
        val nodesWithoutWhitespace = node.children().filter { it.elementType != KtTokens.WHITE_SPACE }.toList()
        val secondArgumentNodes = kotlinScanner.scanNodes(listOf(nodesWithoutWhitespace[2]), ScannerState.STATEMENT)
        val innerTokens = listOf(
            *kotlinScanner.scanNodes(listOf(nodesWithoutWhitespace[0]), ScannerState.STATEMENT).toTypedArray(),
            nonBreakingSpaceToken(content = " "),
            *kotlinScanner.scanNodes(listOf(nodesWithoutWhitespace[1]), ScannerState.STATEMENT).toTypedArray(),
            WhitespaceToken(
                length = 1 + lengthOfTokensForWhitespace(secondArgumentNodes),
                content = " "
            ),
            *secondArgumentNodes.toTypedArray()
        )
        return inBeginEndBlock(innerTokens, State.CODE)
    }
}
