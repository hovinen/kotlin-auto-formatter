package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
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
            anyNode()
            whitespace()
            nodeOfType(KtTokens.IDENTIFIER)
            zeroOrOne { nodeOfType(KtNodeTypes.VALUE_PARAMETER_LIST) }
            zeroOrOne {
                possibleWhitespace()
                nodeOfType(KtTokens.COLON)
                possibleWhitespace()
                nodeOfType(KtNodeTypes.TYPE_REFERENCE)
                zeroOrMore {
                    whitespace()
                    nodeOfType(KtNodeTypes.PROPERTY_ACCESSOR)
                }
            }
        } andThen { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
        }
        zeroOrOne {
            possibleWhitespace()
            nodeOfType(KtTokens.EQ)
            possibleWhitespace()
            zeroOrMore { anyNode() } andThen { nodes ->
                val tokens = kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                listOf(
                    nonBreakingSpaceToken(content = " "),
                    LeafNodeToken("="),
                    WhitespaceToken(length = 1 + lengthOfTokens(tokens), content = " "),
                    *tokens.toTypedArray()
                )
            }
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(nodePattern.matchSequence(node.children().asIterable()), State.CODE)
}
