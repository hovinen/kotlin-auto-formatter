package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.ClosingForcedBreakToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.MarkerToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.nonBreakingSpaceToken
import org.kotlin.formatter.scanning.nodepattern.NodePattern
import org.kotlin.formatter.scanning.nodepattern.NodePatternBuilder
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for `try`-`catch` expressions. */
internal class TryScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern =
        nodePattern {
            nodeOfType(KtTokens.TRY_KEYWORD) thenMapToTokens { listOf(LeafNodeToken("try ")) }
            possibleWhitespace()
            anyNode() thenMapToTokens { nodes ->
                blockPattern.matchSequence(nodes.first().children().asIterable())
            }
            zeroOrMore {
                possibleCommentBeforeClause()
                nodeOfType(KtNodeTypes.CATCH) thenMapToTokens { nodes ->
                    catchPattern.matchSequence(nodes.first().children().asIterable())
                }
            }
            zeroOrOne {
                possibleCommentBeforeClause()
                nodeOfType(KtNodeTypes.FINALLY) thenMapToTokens { nodes ->
                    finallyPattern.matchSequence(nodes.first().children().asIterable())
                }
            }
            end()
        }

    private val blockPattern =
        nodePattern {
            nodeOfType(KtTokens.LBRACE) thenMapToTokens { listOf(LeafNodeToken("{")) }
            possibleWhitespace()
            zeroOrMoreFrugal { anyNode() } thenMapToTokens { nodes ->
                if (nodes.isNotEmpty()) {
                    inBeginEndBlock(
                        listOf(ForcedBreakToken(count = 1))
                            .plus(kotlinScanner.scanNodes(nodes, ScannerState.BLOCK))
                            .plus(ClosingForcedBreakToken),
                        State.CODE
                    )
                } else {
                    listOf()
                }
            }
            possibleWhitespace()
            nodeOfType(KtTokens.RBRACE) thenMapToTokens { listOf(LeafNodeToken("}")) }
            end()
        }

    private val catchPattern: NodePattern =
        nodePattern {
            nodeOfType(KtTokens.CATCH_KEYWORD) thenMapToTokens {
                listOf(MarkerToken, LeafNodeToken("catch "))
            }
            possibleWhitespace()
            nodeOfType(KtNodeTypes.VALUE_PARAMETER_LIST) thenMapToTokens { nodes ->
                kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
            }
            possibleWhitespace() thenMapToTokens { listOf(nonBreakingSpaceToken()) }
            anyNode() thenMapToTokens { nodes ->
                blockPattern.matchSequence(nodes.first().children().asIterable())
            }
            end()
        }

    private val finallyPattern: NodePattern =
        nodePattern {
            nodeOfType(KtTokens.FINALLY_KEYWORD) thenMapToTokens {
                listOf(MarkerToken, LeafNodeToken("finally"))
            }
            possibleWhitespace() thenMapToTokens { listOf(nonBreakingSpaceToken()) }
            anyNode() thenMapToTokens { nodes ->
                blockPattern.matchSequence(nodes.first().children().asIterable())
            }
            end()
        }

    private fun NodePatternBuilder.possibleCommentBeforeClause() {
        either {
            possibleWhitespace() thenMapToTokens { listOf(ClosingForcedBreakToken) }
            comment()
            possibleWhitespace() thenMapToTokens { listOf(ClosingForcedBreakToken) }
        } or { possibleWhitespace() thenMapToTokens { listOf(nonBreakingSpaceToken()) } }
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(nodePattern.matchSequence(node.children().asIterable()), State.CODE)
}
