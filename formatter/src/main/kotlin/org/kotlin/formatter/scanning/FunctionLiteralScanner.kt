package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.BeginWeakToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.nonBreakingSpaceToken
import org.kotlin.formatter.scanning.nodepattern.NodePatternBuilder
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for anonymous function literals, i.e. lambda expressions. */
internal class FunctionLiteralScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern =
        nodePattern {
            nodeOfType(KtTokens.LBRACE) thenMapToTokens {
                listOf(BeginWeakToken(), BeginToken(State.CODE), LeafNodeToken("{"))
            }
            zeroOrOne {
                possibleWhitespace()
                exactlyOne {
                    nodeOfType(KtNodeTypes.VALUE_PARAMETER_LIST) thenMapToTokens { nodes ->
                        listOf(WhitespaceToken(" "))
                            .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
                    }
                    possibleWhitespace()
                    nodeOfType(KtTokens.ARROW) thenMapToTokens {
                        listOf(SynchronizedBreakToken(whitespaceLength = 1), LeafNodeToken("->"))
                    }
                } thenMapTokens { tokens -> inBeginEndBlock(tokens, State.CODE) }
                possibleWhitespace()
                zeroOrOne { emptyBlock() thenMapToTokens { listOf(nonBreakingSpaceToken()) } }
            } thenMapTokens { it.plus(EndToken) }
            zeroOrOne {
                either {
                    possibleWhitespace()
                    emptyBlock()
                    possibleWhitespace()
                } or {
                    possibleWhitespace() thenMapToTokens { nodes ->
                        val content = nodes.firstOrNull()?.text ?: " "
                        listOf(SynchronizedBreakToken(content = content, whitespaceLength = 1))
                    }
                    nodeOfType(KtNodeTypes.BLOCK) thenMapToTokens { nodes ->
                        kotlinScanner.scanNodes(nodes, ScannerState.BLOCK)
                    }
                    possibleWhitespace() thenMapToTokens { nodes ->
                        val content = nodes.firstOrNull()?.text ?: " "
                        listOf(
                            ClosingSynchronizedBreakToken(content = content, whitespaceLength = 1)
                        )
                    }
                }
            }
            possibleWhitespace()
            nodeOfType(KtTokens.RBRACE) thenMapToTokens { listOf(LeafNodeToken("}"), EndToken) }
            end()
        }

    private fun NodePatternBuilder.emptyBlock() =
        nodeMatching { it.elementType == KtNodeTypes.BLOCK && it.children().toList().isEmpty() }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
