package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for array initializers in annotation arguments. */
internal class CollectionLiteralExpressionScanner(private val kotlinScanner: KotlinScanner) :
    NodeScanner {

    private val nodePattern =
        nodePattern {
            nodeOfType(KtTokens.LBRACKET) thenMapToTokens { listOf(LeafNodeToken("[")) }
            possibleWhitespace()
            either {
                oneOrMoreFrugal { anyNode() } thenMapToTokens { nodes ->
                    listOf(SynchronizedBreakToken(whitespaceLength = 0))
                        .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
                }
                possibleWhitespace()
                nodeOfType(KtTokens.COMMA) thenMapToTokens { listOf(LeafNodeToken(",")) }
                possibleWhitespace()
                zeroOrMore {
                    oneOrMoreFrugal { anyNode() } thenMapToTokens { nodes ->
                        listOf(SynchronizedBreakToken(whitespaceLength = 1))
                            .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
                    }
                    possibleWhitespace()
                    nodeOfType(KtTokens.COMMA) thenMapToTokens { listOf(LeafNodeToken(",")) }
                    possibleWhitespace()
                }
                oneOrMoreFrugal { anyNode() } thenMapToTokens { nodes ->
                    listOf(SynchronizedBreakToken(whitespaceLength = 1))
                        .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
                }
            } or {
                zeroOrMoreFrugal { anyNode() } thenMapToTokens { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                }
            }
            possibleWhitespace()
            nodeOfType(KtTokens.RBRACKET) thenMapToTokens {
                listOf(ClosingSynchronizedBreakToken(whitespaceLength = 0), LeafNodeToken("]"))
            }
            end()
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
