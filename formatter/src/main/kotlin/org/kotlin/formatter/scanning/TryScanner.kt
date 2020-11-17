package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.ClosingForcedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.MarkerToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.nonBreakingSpaceToken
import org.kotlin.formatter.scanning.nodepattern.NodePattern
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
            oneOrMore {
                possibleWhitespace()
                nodeOfType(KtNodeTypes.CATCH) thenMapToTokens { nodes ->
                    listOf(nonBreakingSpaceToken())
                        .plus(catchPattern.matchSequence(nodes.first().children().asIterable()))
                }
            }
            end()
        }

    private val blockPattern =
        nodePattern {
            nodeOfType(KtTokens.LBRACE) thenMapToTokens {
                listOf(LeafNodeToken("{"), BeginToken(State.CODE), ForcedBreakToken(count = 1))
            }
            possibleWhitespace()
            oneOrMoreFrugal { anyNode() } thenMapToTokens { nodes ->
                kotlinScanner.scanNodes(nodes, ScannerState.BLOCK)
            }
            possibleWhitespace()
            nodeOfType(KtTokens.RBRACE) thenMapToTokens {
                listOf(ClosingForcedBreakToken, EndToken, LeafNodeToken("}"))
            }
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

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(nodePattern.matchSequence(node.children().asIterable()), State.CODE)
}
