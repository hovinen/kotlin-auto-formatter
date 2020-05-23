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
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for `for` loop expressions. */
internal class ForExpressionScanner(private val kotlinScanner: KotlinScanner): NodeScanner {
    private val nodePattern =
        nodePattern {
            nodeOfType(KtTokens.FOR_KEYWORD)
            possibleWhitespace()
            nodeOfType(KtTokens.LPAR)
            oneOrMore { anyNode() } thenMapToTokens { nodes ->
                val tokens = kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                listOf(LeafNodeToken("for ("), BeginToken(State.CODE)).plus(tokens)
                    .plus(ClosingSynchronizedBreakToken(whitespaceLength = 0))
                    .plus(EndToken)
                    .plus(LeafNodeToken(")"))
            }
            nodeOfType(KtTokens.RPAR)
            zeroOrMore { anyNode() } thenMapToTokens { nodes ->
                kotlinScanner.scanNodes(nodes, ScannerState.BLOCK)
            }
            end()
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(nodePattern.matchSequence(node.children().asIterable()), State.CODE)
}
