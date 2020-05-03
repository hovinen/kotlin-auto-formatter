package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.ClosingForcedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.nonBreakingSpaceToken
import org.kotlin.formatter.scanning.nodepattern.NodePatternBuilder
import org.kotlin.formatter.scanning.nodepattern.nodePattern

internal class PropertyScanner(private val kotlinScanner: KotlinScanner): NodeScanner {
    private val nodePattern = nodePattern {
        exactlyOne {
            either {
                exactlyOne {
                    nodeOfType(KtNodeTypes.MODIFIER_LIST) andThen { nodes ->
                        kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                    }
                    possibleWhitespace()
                    anyNode() andThen { nodes ->
                        kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                    }
                } thenMapTokens { tokens ->
                    insertWhitespaceIfNoForcedBreakIsPresent(tokens)
                }
                zeroOrMoreFrugal { anyNode() } andThen { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                }
            } or {
                oneOrMoreFrugal { anyNode() } andThen { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                }
            }
            zeroOrOne { propertyInitializer(kotlinScanner) }
        } thenMapTokens { inBeginEndBlock(it, State.CODE) }
        end()
    }

    private fun insertWhitespaceIfNoForcedBreakIsPresent(tokens: List<Token>): List<Token> {
        return if (tokens[tokens.size - 2] is ClosingForcedBreakToken) {
            tokens
        } else {
            listOf(
                *tokens.subList(0, tokens.size - 1).toTypedArray(),
                WhitespaceToken(length = lengthOfTokens(listOf(tokens.last())), content = " "),
                tokens.last()
            )
        }
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}

internal fun NodePatternBuilder.propertyInitializer(kotlinScanner: KotlinScanner) {
    possibleWhitespace()
    nodeOfType(KtTokens.EQ)
    possibleWhitespace()
    zeroOrMore { anyNode() } andThen { nodes ->
        val tokens = kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
        listOf(
            nonBreakingSpaceToken(),
            LeafNodeToken("="),
            WhitespaceToken(length = 1 + lengthOfTokens(tokens), content = " "),
            *tokens.toTypedArray()
        )
    }
}
