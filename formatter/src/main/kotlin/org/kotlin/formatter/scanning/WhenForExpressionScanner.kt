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
import org.kotlin.formatter.scanning.nodepattern.nodePattern

internal class WhenForExpressionScanner(private val kotlinScanner: KotlinScanner): NodeScanner {
    private val whenForPattern = nodePattern {
        oneOrMore { anyNode() } andThen { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
        }
        nodeOfType(KtTokens.LPAR)
        oneOrMore { anyNode() } andThen { nodes ->
            val tokens = kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
            listOf(
                LeafNodeToken("("),
                BeginToken(State.CODE),
                *tokens.toTypedArray(),
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                EndToken,
                LeafNodeToken(")")
            )
        }
        nodeOfType(KtTokens.RPAR)
        zeroOrMore { anyNode() } andThen { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.BLOCK)
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(whenForPattern.matchSequence(node.children().asIterable()), State.CODE)
}
