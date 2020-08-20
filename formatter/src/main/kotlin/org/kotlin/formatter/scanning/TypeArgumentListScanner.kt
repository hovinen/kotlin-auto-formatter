package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
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

/** A [NodeScanner] for a list of type arguments. */
internal class TypeArgumentListScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern =
        nodePattern {
            nodeOfType(KtTokens.LT) thenMapToTokens { listOf(LeafNodeToken("<")) }
            possibleWhitespaceWithComment()
            either {
                exactlyOne {
                    nodeOfType(KtNodeTypes.TYPE_PROJECTION) thenMapToTokens { nodes ->
                        listOf(SynchronizedBreakToken(whitespaceLength = 0))
                            .plus(
                                inBeginEndBlock(
                                    kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT),
                                    State.CODE
                                )
                            )
                    }
                    possibleWhitespace()
                    nodeOfType(KtTokens.COMMA) thenMapToTokens { listOf(LeafNodeToken(",")) }
                }
                possibleWhitespace()
                zeroOrMore {
                    exactlyOne {
                        nodeOfType(KtNodeTypes.TYPE_PROJECTION) thenMapToTokens { nodes ->
                            listOf(SynchronizedBreakToken(whitespaceLength = 1))
                                .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
                        }
                        possibleWhitespace()
                        nodeOfType(KtTokens.COMMA) thenMapToTokens { listOf(LeafNodeToken(",")) }
                    }
                    possibleWhitespaceWithComment()
                }
                exactlyOne { nodeOfType(KtNodeTypes.TYPE_PROJECTION) } thenMapToTokens { nodes ->
                    listOf(SynchronizedBreakToken(whitespaceLength = 1))
                        .plus(
                            inBeginEndBlock(
                                kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT),
                                State.CODE
                            )
                        )
                }
            } or {
                zeroOrOne {
                    exactlyOne { nodeOfType(KtNodeTypes.TYPE_PROJECTION) } thenMapToTokens
                        { nodes ->
                            listOf(SynchronizedBreakToken(whitespaceLength = 0))
                                .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
                        }
                }
            }
            zeroOrOne { nodeOfType(KtTokens.COMMA) thenMapToTokens { listOf(LeafNodeToken(",")) } }
            possibleWhitespaceWithComment(ignoreTrailingWhitespace = true)
            nodeOfType(KtTokens.GT) thenMapToTokens {
                listOf(ClosingSynchronizedBreakToken(whitespaceLength = 0), LeafNodeToken(">"))
            }
            end()
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(nodePattern.matchSequence(node.children().asIterable()), State.CODE)
}
