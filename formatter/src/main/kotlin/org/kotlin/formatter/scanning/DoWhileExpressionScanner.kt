package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.ClosingForcedBreakToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.scanning.nodepattern.NodePattern
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for `do`-`while` loops. */
internal class DoWhileExpressionScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern: NodePattern =
        nodePattern {
            nodeOfType(KtTokens.DO_KEYWORD) thenMapToTokens { listOf(LeafNodeToken("do ")) }
            possibleWhitespace()
            nodeOfType(KtNodeTypes.BODY) thenMapToTokens { nodes ->
                inBeginEndBlock(
                    bodyPattern.matchSequence(nodes.first().children().asIterable()),
                    State.CODE
                )
            }
            possibleWhitespace()
            nodeOfType(KtTokens.WHILE_KEYWORD) thenMapToTokens { listOf(LeafNodeToken(" while (")) }
            possibleWhitespace()
            nodeOfType(KtTokens.LPAR)
            possibleWhitespace()
            nodeOfType(KtNodeTypes.CONDITION) thenMapToTokens { nodes ->
                kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
            }
            possibleWhitespace()
            nodeOfType(KtTokens.RPAR) thenMapToTokens { listOf(LeafNodeToken(")")) }
            zeroOrOne {
                possibleWhitespace()
                nodeOfType(KtTokens.SEMICOLON)
            }
            end()
        }

    private val bodyPattern: NodePattern =
        nodePattern {
            either {
                nodeOfType(KtNodeTypes.BLOCK) thenMapToTokens { nodes ->
                    blockPattern.matchSequence(nodes.first().children().asIterable())
                }
            } or {
                zeroOrMore { anyNode() } thenMapToTokens { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.BLOCK)
                }
            }
            end()
        }

    private val blockPattern: NodePattern =
        nodePattern {
            nodeOfType(KtTokens.LBRACE) thenMapToTokens {
                listOf(LeafNodeToken("{"), ForcedBreakToken(count = 1))
            }
            possibleWhitespace()
            zeroOrMoreFrugal { anyNode() } thenMapToTokens { nodes ->
                kotlinScanner.scanNodes(nodes, ScannerState.BLOCK)
            }
            possibleWhitespace()
            nodeOfType(KtTokens.RBRACE) thenMapToTokens {
                listOf(ClosingForcedBreakToken, LeafNodeToken("}"))
            }
            end()
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
