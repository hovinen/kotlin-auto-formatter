package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.nonBreakingSpaceToken

internal class BinaryExpressionScanner(private val kotlinScanner: KotlinScanner): NodeScanner {
    private val expressionPattern = nodePattern {
        accumulateUntilNodeMatching({ true }) { _, firstNode ->
            listOf(
                *kotlinScanner.scanNodes(listOf(firstNode), ScannerState.STATEMENT).toTypedArray(),
                nonBreakingSpaceToken(content = " ")
            )
        }
        accumulateUntilNodeMatching({ node -> node.elementType != KtTokens.WHITE_SPACE })
        { _, operator ->
            kotlinScanner.scanNodes(listOf(operator), ScannerState.STATEMENT)
        }
        skipNodesMatching { node -> node.elementType == KtTokens.WHITE_SPACE }
        accumulateUntilEnd { nodes, _ ->
            val tokens = kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
            listOf(
                WhitespaceToken(length = 1 + lengthOfTokensForWhitespace(tokens), content = " "),
                *tokens.toTypedArray()
            )
        }
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(expressionPattern.matchSequence(node.children().toList()), State.CODE)
}
