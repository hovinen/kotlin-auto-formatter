package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.nonBreakingSpaceToken
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for `return` expressions. */
internal class ReturnScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern =
        nodePattern {
            nodeOfType(KtTokens.RETURN_KEYWORD) thenMapToTokens { listOf(LeafNodeToken("return")) }
            zeroOrOne {
                nodeOfType(KtNodeTypes.LABEL_QUALIFIER) thenMapToTokens { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                }
            }
            possibleWhitespace()
            zeroOrMore { anyNode() } thenMapToTokens { nodes ->
                if (nodes.isNotEmpty()) {
                    listOf(nonBreakingSpaceToken()).plus(
                        kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                    )
                } else {
                    listOf()
                }
            }
            end()
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(nodePattern.matchSequence(node.children().asIterable()), State.CODE)
}
