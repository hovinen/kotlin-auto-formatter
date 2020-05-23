package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for `while` expressions. */
internal class WhileExpressionScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern =
        nodePattern {
            nodeOfType(KtTokens.WHILE_KEYWORD)
            possibleWhitespace()
            nodeOfType(KtTokens.LPAR)
            oneOrMore { anyNode() } thenMapToTokens { nodes ->
                listOf(LeafNodeToken("while ("))
                    .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
                    .plus(LeafNodeToken(")"))
            }
            nodeOfType(KtTokens.RPAR)
            zeroOrMore { anyNode() } thenMapToTokens { nodes ->
                kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
            }
            end()
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(nodePattern.matchSequence(node.children().asIterable()), State.CODE)
}
