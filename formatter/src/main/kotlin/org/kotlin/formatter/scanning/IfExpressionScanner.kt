package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.ClosingForcedBreakToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.scanning.nodepattern.NodePattern
import org.kotlin.formatter.scanning.nodepattern.NodePatternBuilder
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for `if` expressions. */
internal class IfExpressionScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern =
        nodePattern {
            nodeOfType(KtTokens.IF_KEYWORD)
            possibleWhitespace()
            nodeOfType(KtTokens.LPAR)
            possibleWhitespace()
            nodeOfType(KtNodeTypes.CONDITION) thenMapToTokens { nodes ->
                listOf(LeafNodeToken("if ("))
                    .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
            }
            possibleWhitespace()
            nodeOfType(KtTokens.RPAR) thenMapToTokens { listOf(LeafNodeToken(")")) }
            possibleWhitespace()
            either {
                possibleWhitespace()
                possibleComment() thenMapTokens { tokens ->
                    tokens.ifEmpty { listOf(LeafNodeToken(" ")) }
                }
                possibleWhitespace()
                thenWithBlock() thenMapToTokens { nodes ->
                    thenWithBlockPattern.matchSequence(nodes.first().children().asIterable())
                }
                zeroOrOne {
                    possibleWhitespace()
                    nodeOfType(KtTokens.ELSE_KEYWORD)
                    possibleWhitespace()
                    nodeOfType(KtNodeTypes.ELSE) thenMapToTokens { nodes ->
                        elsePatternFollowingBlock.matchSequence(
                            nodes.first().children().asIterable()
                        )
                    }
                }
            } or {
                possibleWhitespaceWithComment() thenMapTokens { tokens ->
                    if (tokens.isNotEmpty()) {
                        listOf(WhitespaceToken(content = " ")).plus(tokens)
                    } else {
                        listOf()
                    }
                }
                possibleWhitespace()
                nodeOfType(KtNodeTypes.THEN) thenMapToTokens { nodes ->
                    thenWithoutBlockPattern.matchSequence(nodes.first().children().asIterable())
                }
                zeroOrOne {
                    possibleWhitespace()
                    possibleComment()
                    possibleWhitespace()
                    nodeOfType(KtTokens.ELSE_KEYWORD) thenMapToTokens {
                        listOf(
                            ClosingSynchronizedBreakToken(whitespaceLength = 1),
                            LeafNodeToken("else"),
                            WhitespaceToken(content = " ")
                        )
                    }
                    possibleWhitespaceWithComment()
                    nodeOfType(KtNodeTypes.ELSE) thenMapToTokens { nodes ->
                        elsePattern.matchSequence(nodes.first().children().asIterable())
                    }
                }
            }
            end()
        }

    private fun NodePatternBuilder.possibleComment(): NodePatternBuilder =
        zeroOrOne {
            comment() thenMapTokens { tokens ->
                listOf(WhitespaceToken(" ")).plus(tokens).plus(ClosingForcedBreakToken)
            }
        }

    private fun NodePatternBuilder.thenWithBlock(): NodePatternBuilder =
        nodeMatching {
            it.elementType == KtNodeTypes.THEN && it.firstChildNode.elementType == KtNodeTypes.BLOCK
        }

    private val thenWithBlockPattern: NodePattern =
        nodePattern {
            nodeOfType(KtNodeTypes.BLOCK) thenMapToTokens { nodes ->
                kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
            }
            end()
        }

    private val thenWithoutBlockPattern: NodePattern =
        nodePattern {
            oneOrMore { anyNode() } thenMapToTokens { nodes ->
                listOf(SynchronizedBreakToken(whitespaceLength = 1))
                    .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
            }
            end()
        }

    private val elsePattern: NodePattern =
        nodePattern {
            either {
                nodeOfOneOfTypes(KtNodeTypes.BLOCK, KtNodeTypes.IF) thenMapToTokens { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                }
            } or {
                oneOrMore { anyNode() } thenMapToTokens { nodes ->
                    listOf(SynchronizedBreakToken(whitespaceLength = 0))
                        .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
                }
            }
            end()
        }

    private val elsePatternFollowingBlock: NodePattern =
        nodePattern {
            either {
                nodeOfOneOfTypes(KtNodeTypes.BLOCK, KtNodeTypes.IF) thenMapToTokens { nodes ->
                    listOf(LeafNodeToken(" else "))
                        .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
                }
            } or {
                oneOrMore { anyNode() } thenMapToTokens { nodes ->
                    listOf(LeafNodeToken(" else"), SynchronizedBreakToken(whitespaceLength = 1))
                        .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
                }
            }
            end()
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(nodePattern.matchSequence(node.children().asIterable()), State.CODE)
}
