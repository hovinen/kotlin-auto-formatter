package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.nonBreakingSpaceToken

internal class PropertyScanner(private val kotlinScanner: KotlinScanner) {
    fun tokensForProperty(node: ASTNode): List<Token> {
        val childNodes = node.children().toList()
        val indexOfAssignmentOperator = childNodes.indexOfFirst { it.elementType == KtTokens.EQ }
        if (indexOfAssignmentOperator == -1) {
            return kotlinScanner.tokensForBlockNode(node, State.CODE, ScannerState.STATEMENT)
        } else {
            var lastNonWhitespaceIndex = indexOfAssignmentOperator - 1
            while (childNodes[lastNonWhitespaceIndex].elementType == KtTokens.WHITE_SPACE) {
                lastNonWhitespaceIndex--
            }
            val nodesUpToAssignmentOperator = childNodes.subList(0, lastNonWhitespaceIndex + 1)
            val tokensUpToAssignmentOperator = kotlinScanner.scanNodes(nodesUpToAssignmentOperator, ScannerState.STATEMENT)
            var firstNonWhitespaceIndex = indexOfAssignmentOperator + 1
            while (childNodes[firstNonWhitespaceIndex].elementType == KtTokens.WHITE_SPACE) {
                firstNonWhitespaceIndex++
            }
            val nodesAfterAssignmentOperator = childNodes.subList(firstNonWhitespaceIndex, childNodes.size)
            val tokensAfterAssignmentOperator = kotlinScanner.scanNodes(nodesAfterAssignmentOperator, ScannerState.STATEMENT)
            val innerTokens = listOf(
                *tokensUpToAssignmentOperator.toTypedArray(),
                nonBreakingSpaceToken(content = " "),
                LeafNodeToken("="),
                WhitespaceToken(
                    length = 1 + lengthOfTokens(tokensAfterAssignmentOperator),
                    content = " "
                ),
                *tokensAfterAssignmentOperator.toTypedArray()
            )
            return inBeginEndBlock(innerTokens, State.CODE)
        }
    }
}