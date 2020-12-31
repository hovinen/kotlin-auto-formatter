package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.BlockFromMarkerToken
import org.kotlin.formatter.ClosingForcedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.MarkerToken
import org.kotlin.formatter.NonIndentingSynchronizedBreakToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.nonBreakingSpaceToken
import org.kotlin.formatter.scanning.nodepattern.NodePattern
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for class and interface definitions. */
internal class ClassScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val modifierListScanner =
        ModifierListScanner(
            kotlinScanner = kotlinScanner,
            markerCount = 1,
            breakMode = ModifierListScanner.BreakMode.TYPE
        )
    private val nodePattern =
        nodePattern {
            optionalKDoc(kotlinScanner, modifierListScanner)
            possibleWhitespaceWithComment()
            zeroOrOne {
                nodeOfType(KtNodeTypes.MODIFIER_LIST) thenMapToTokens { nodes ->
                    modifierListScanner.scan(nodes.first(), ScannerState.STATEMENT)
                }
                possibleWhitespace()
                zeroOrOne {
                    comment()
                    possibleWhitespaceOutputToToken() thenMapTokens { tokens ->
                        tokens.plus(MarkerToken)
                    }
                }
            } thenMapTokens { tokens -> listOf(MarkerToken).plus(tokens) }
            exactlyOne {
                nodeOfOneOfTypes(
                    KtTokens.CLASS_KEYWORD,
                    KtTokens.OBJECT_KEYWORD,
                    KtTokens.INTERFACE_KEYWORD
                )
                zeroOrMoreFrugal { anyNode() }
            } thenMapToTokens { nodes -> kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT) }
            possibleWhitespace()
            zeroOrOne {
                nodeOfType(KtNodeTypes.PRIMARY_CONSTRUCTOR) thenMapToTokens { nodes ->
                    val prefix =
                        if (!nodes.first().text.startsWith("(")) {
                            listOf(WhitespaceToken(" "))
                        } else {
                            listOf()
                        }
                    prefix.plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
                }
                zeroOrMoreFrugal { anyNode() } thenMapToTokens { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                }
            }
            possibleWhitespace()
            either {
                either {
                    nodeOfType(KtTokens.COLON)
                    possibleWhitespace()
                    nodeOfType(KtNodeTypes.SUPER_TYPE_LIST) thenMapToTokens { nodes ->
                        listOf(LeafNodeToken(" :"))
                            .plus(
                                inBeginEndBlock(
                                    listOf(WhitespaceToken(" "))
                                        .plus(
                                            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                                        )
                                        .plus(LeafNodeToken(" {"))
                                        .plus(
                                            NonIndentingSynchronizedBreakToken(whitespaceLength = 0)
                                        ),
                                    State.CODE
                                )
                            )
                    }
                    possibleWhitespace()
                    nodeOfType(KtNodeTypes.CLASS_BODY) thenMapToTokens { nodes ->
                        classBodyNodePattern.matchSequence(nodes.first().children().asIterable())
                    }
                } or {
                    nodeOfType(KtTokens.COLON)
                    possibleWhitespace()
                    nodeOfType(KtNodeTypes.SUPER_TYPE_LIST) thenMapToTokens { nodes ->
                        listOf(LeafNodeToken(" :"))
                            .plus(
                                inBeginEndBlock(
                                    listOf(WhitespaceToken(" "))
                                        .plus(
                                            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                                        ),
                                    State.CODE
                                )
                            )
                    }
                }
            } or {
                zeroOrOne { nodeOfType(KtNodeTypes.CLASS_BODY) } thenMapToTokens { nodes ->
                    if (nodes.isNotEmpty()) {
                        listOf(nonBreakingSpaceToken())
                            .plus(kotlinScanner.scanNodes(nodes, ScannerState.BLOCK))
                    } else {
                        listOf(BlockFromMarkerToken)
                    }
                }
            }
            end()
        }

    private val classBodyNodePattern: NodePattern =
        nodePattern {
            nodeOfType(KtTokens.LBRACE) thenMapToTokens {
                listOf(BlockFromMarkerToken, BeginToken(State.CODE))
            }
            zeroOrOne {
                whitespaceWithNewline() thenMapToTokens { nodes ->
                    if (nodes.first().text.hasDoubleNewline) {
                        listOf(ForcedBreakToken(count = 1))
                    } else {
                        kotlinScanner.scanNodes(nodes, ScannerState.BLOCK)
                    }
                }
                oneOrMoreFrugal { anyNode() } thenMapToTokens { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.BLOCK)
                }
            }
            zeroOrOne { whitespaceWithNewline() } thenMapToTokens { nodes ->
                if (nodes.isNotEmpty()) listOf(ClosingForcedBreakToken) else listOf()
            }
            nodeOfType(KtTokens.RBRACE) thenMapToTokens { listOf(EndToken, LeafNodeToken("}")) }
            end()
        }

    private val String.hasDoubleNewline: Boolean
        get() = count { it == '\n' } > 1

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
