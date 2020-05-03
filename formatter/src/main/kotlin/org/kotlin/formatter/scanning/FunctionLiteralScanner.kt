package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.scanning.nodepattern.nodePattern

internal class FunctionLiteralScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern = nodePattern {
        nodeOfType(KtTokens.LBRACE)
        possibleWhitespace()
        zeroOrMoreFrugal { anyNode() } andThen { nodes ->
            listOf(
                LeafNodeToken("{"),
                WhitespaceToken(content = " "),
                *kotlinScanner.scanNodes(nodes, ScannerState.BLOCK).toTypedArray(),
                ClosingSynchronizedBreakToken(whitespaceLength = 1),
                LeafNodeToken("}")
            )
        }
        possibleWhitespace()
        nodeOfType(KtTokens.RBRACE)
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(nodePattern.matchSequence(node.children().asIterable()), State.CODE)
}
