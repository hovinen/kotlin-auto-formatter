package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.BlockFromMarkerToken
import org.kotlin.formatter.ClosingForcedBreakToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for block statements and class content. */
internal class BlockScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern =
        nodePattern {
            either {
                nodeOfType(KtTokens.LBRACE) thenMapToTokens {
                    listOf(LeafNodeToken("{"), BlockFromMarkerToken, BeginToken(State.CODE))
                }
                zeroOrOne { whitespaceWithNewline() } thenMapToTokens { nodes ->
                    if (nodes.isNotEmpty()) {
                        listOf(ForcedBreakToken(count = nodes.first().text.count { it == '\n' }))
                    } else {
                        listOf(SynchronizedBreakToken(whitespaceLength = 0))
                    }
                }
                zeroOrMoreFrugal { anyNode() } thenMapToTokens { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.BLOCK)
                }
                zeroOrOne { whitespaceWithNewline() } thenMapToTokens { nodes ->
                    if (nodes.isNotEmpty()) {
                        listOf(ClosingForcedBreakToken)
                    } else {
                        listOf(ClosingSynchronizedBreakToken(whitespaceLength = 0))
                    }
                }
                nodeOfType(KtTokens.RBRACE) thenMapToTokens { listOf(EndToken, LeafNodeToken("}")) }
            } or {
                zeroOrMoreFrugal { anyNode() } thenMapToTokens { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.BLOCK)
                }
                zeroOrOne { whitespaceWithNewline() } thenMapToTokens { nodes ->
                    if (nodes.isNotEmpty()) listOf(ClosingForcedBreakToken) else listOf()
                }
            }
            end()
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
