package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.BlockFromMarkerToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.MarkerToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/**
 * A [NodeScanner] for a list of parameters in a function or class declaration, or the arguments of
 * a function call expression.
 */
internal class ParameterListScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val modifierListScanner =
        ModifierListScanner(kotlinScanner, breakMode = ModifierListScanner.BreakMode.PARAMETER)
    private val parameterListPattern =
        nodePattern {
            either {
                zeroOrOne {
                    nodeOfType(KtTokens.LPAR) thenMapToTokens {
                        listOf(LeafNodeToken("("), BeginToken(State.CODE))
                    }
                }
                possibleWhitespaceWithComment()
                nodeOfOneOfTypes(
                    KtNodeTypes.VALUE_PARAMETER,
                    KtNodeTypes.VALUE_ARGUMENT
                ) thenMapToTokens { nodes ->
                    listOf(SynchronizedBreakToken(whitespaceLength = 0)).plus(
                        parameterPattern.matchSequence(nodes.first().children().asIterable())
                    )
                }
                zeroOrMore {
                    possibleWhitespaceWithComment()
                    nodeOfType(KtTokens.COMMA) thenMapToTokens { listOf(LeafNodeToken(",")) }
                    either {
                        possibleWhitespace() thenMapToTokens { nodes ->
                            listOf(SynchronizedBreakToken(whitespaceLength = 1))
                        }
                    } or {
                        either {
                            possibleWhitespace() thenMapToTokens { nodes ->
                                val newlineCount =
                                    nodes.firstOrNull()?.text?.count { it == '\n' } ?: 0
                                if (newlineCount > 0) {
                                    listOf(ForcedBreakToken(count = newlineCount))
                                } else {
                                    listOf(WhitespaceToken(" "))
                                }
                            }
                            comment()
                            whitespaceWithNewline() thenMapToTokens { nodes ->
                                listOf(SynchronizedBreakToken(whitespaceLength = 1))
                            }
                        } or {
                            possibleWhitespace() thenMapToTokens { nodes ->
                                val newlineCount =
                                    nodes.firstOrNull()?.text?.count { it == '\n' } ?: 0
                                if (newlineCount > 1) {
                                    listOf(ForcedBreakToken(count = newlineCount))
                                } else {
                                    listOf(SynchronizedBreakToken(whitespaceLength = 1))
                                }
                            }
                            comment()
                            possibleWhitespace() thenMapToTokens { nodes ->
                                listOf(WhitespaceToken(" "))
                            }
                        }
                    }
                    nodeOfOneOfTypes(
                        KtNodeTypes.VALUE_PARAMETER,
                        KtNodeTypes.VALUE_ARGUMENT
                    ) thenMapToTokens { nodes ->
                        parameterPattern.matchSequence(nodes.first().children().asIterable())
                    }
                }
                possibleWhitespaceWithComment()
                zeroOrOne {
                    nodeOfType(KtTokens.RPAR) thenMapToTokens {
                        listOf(
                            ClosingSynchronizedBreakToken(whitespaceLength = 0),
                            EndToken,
                            LeafNodeToken(")")
                        )
                    }
                }
            } or {
                nodeOfType(KtTokens.LPAR) thenMapToTokens { listOf(LeafNodeToken("(")) }
                possibleWhitespaceWithComment()
                nodeOfType(KtTokens.RPAR) thenMapToTokens { listOf(LeafNodeToken(")")) }
            }
            end()
        }

    private val parameterPattern =
        nodePattern {
            zeroOrOne {
                nodeOfType(KtNodeTypes.MODIFIER_LIST) thenMapToTokens { nodes ->
                    modifierListScanner.scan(nodes.first(), ScannerState.STATEMENT)
                }
                possibleWhitespace()
            } thenMapTokens { tokens ->
                if (tokens.isEmpty()) {
                    listOf(MarkerToken)
                } else {
                    tokens
                }
            }
            oneOrMore { anyNode() } thenMapToTokens { nodes ->
                kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT).plus(BlockFromMarkerToken)
            }
            end()
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        parameterListPattern.matchSequence(node.children().toList())
}
