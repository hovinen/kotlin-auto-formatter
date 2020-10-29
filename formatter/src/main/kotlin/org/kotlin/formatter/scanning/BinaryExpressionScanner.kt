package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.nonBreakingSpaceToken
import org.kotlin.formatter.scanning.nodepattern.NodePatternBuilder
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for binary expressions. */
internal class BinaryExpressionScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val expressionPattern =
        nodePattern {
            either {
                either {
                    oneOrMoreFrugal { anyNode() } thenMapToTokens { firstNode ->
                        kotlinScanner.scanNodes(firstNode, ScannerState.STATEMENT)
                    }
                    possibleWhitespace()
                    rangeOperator() thenMapToTokens { operator ->
                        kotlinScanner.scanNodes(operator, ScannerState.STATEMENT)
                    }
                    possibleWhitespace()
                    oneOrMore { anyNode() } thenMapToTokens { nodes ->
                        kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                    }
                } or {
                    oneOrMoreFrugal { anyNode() } thenMapToTokens { firstNode ->
                        kotlinScanner.scanNodes(firstNode, ScannerState.STATEMENT)
                    }
                    possibleWhitespace() thenMapToTokens { nodes ->
                        if (nodes.isNotEmpty()) {
                            listOf(WhitespaceToken(nodes.first().text))
                        } else {
                            listOf(WhitespaceToken(" "))
                        }
                    }
                    elvisOperator() thenMapToTokens { operator ->
                        listOf(BeginToken(State.CODE))
                            .plus(kotlinScanner.scanNodes(operator, ScannerState.STATEMENT))
                    }
                    possibleWhitespace()
                    oneOrMore { anyNode() } thenMapToTokens { nodes ->
                        listOf(nonBreakingSpaceToken())
                            .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
                            .plus(EndToken)
                    }
                }
            } or {
                oneOrMoreFrugal { anyNode() } thenMapToTokens { firstNode ->
                    kotlinScanner.scanNodes(firstNode, ScannerState.STATEMENT)
                }
                either {
                    possibleWhitespace() thenMapToTokens { listOf(nonBreakingSpaceToken()) }
                } or { possibleWhitespaceWithComment() }
                nodeOfType(KtNodeTypes.OPERATION_REFERENCE) thenMapToTokens { operator ->
                    kotlinScanner.scanNodes(operator, ScannerState.STATEMENT)
                }
                possibleWhitespace() thenMapToTokens { nodes ->
                    if (nodes.isNotEmpty()) {
                        listOf(WhitespaceToken(nodes.first().text))
                    } else {
                        listOf(WhitespaceToken(" "))
                    }
                }
                oneOrMore { anyNode() } thenMapToTokens { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                }
            }
            end()
        }

    private fun NodePatternBuilder.rangeOperator() =
        nodeMatching { it.elementType == KtNodeTypes.OPERATION_REFERENCE && it.text == ".." }

    private fun NodePatternBuilder.elvisOperator() =
        nodeMatching { it.elementType == KtNodeTypes.OPERATION_REFERENCE && it.text == "?:" }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(expressionPattern.matchSequence(node.children().toList()), State.CODE)
}
