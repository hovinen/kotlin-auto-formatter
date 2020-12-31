package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for a type parameter list. */
internal class TypeParameterListScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern =
        nodePattern {
            nodeOfType(KtTokens.LT) thenMapToTokens { listOf(LeafNodeToken("<")) }
            possibleWhitespaceWithComment()
            zeroOrOne {
                nodeOfType(KtNodeTypes.TYPE_PARAMETER) thenMapToTokens { nodes ->
                    listOf(SynchronizedBreakToken(whitespaceLength = 0))
                        .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
                }
                possibleWhitespaceWithComment()
                zeroOrOne {
                    nodeOfType(KtTokens.COMMA) thenMapToTokens { listOf(LeafNodeToken(",")) }
                }
                possibleWhitespaceWithComment()
            }
            zeroOrMore {
                nodeOfType(KtNodeTypes.TYPE_PARAMETER) thenMapToTokens { nodes ->
                    listOf(SynchronizedBreakToken(whitespaceLength = 1))
                        .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
                }
                possibleWhitespaceWithComment()
                zeroOrOne {
                    nodeOfType(KtTokens.COMMA) thenMapToTokens { listOf(LeafNodeToken(",")) }
                }
                possibleWhitespaceWithComment()
            }
            possibleWhitespaceWithComment()
            nodeOfType(KtTokens.GT) thenMapToTokens {
                listOf(ClosingSynchronizedBreakToken(whitespaceLength = 0), LeafNodeToken(">"))
            }
            end()
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
