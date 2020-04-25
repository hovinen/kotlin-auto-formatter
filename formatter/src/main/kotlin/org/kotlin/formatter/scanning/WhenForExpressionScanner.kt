package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token

internal class WhenForExpressionScanner(private val kotlinScanner: KotlinScanner): NodeScanner {
    private val whenForPattern = nodePattern {
        matchingElement({ it.elementType == KtTokens.LPAR }) { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
        }
        matchingElement({ it.elementType == KtTokens.RPAR }) { nodes ->
            val tokens = kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
            listOf(
                LeafNodeToken("("),
                BeginToken(length = lengthOfTokens(tokens), state = State.CODE),
                *tokens.toTypedArray(),
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                EndToken,
                LeafNodeToken(")")
            )
        }
        endingElement { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.BLOCK)
        }
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(whenForPattern.matchSequence(node.children().asIterable()), State.CODE)
}
