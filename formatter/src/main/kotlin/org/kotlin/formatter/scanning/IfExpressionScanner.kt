package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.scanning.nodepattern.nodePattern

internal class IfExpressionScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern = nodePattern {
        exactlyOne {
            nodeOfType(KtTokens.IF_KEYWORD)
            possibleWhitespace()
            nodeOfType(KtTokens.LPAR)
            nodeOfType(KtNodeTypes.CONDITION) andThen { nodes ->
                listOf(
                    LeafNodeToken("if ("),
                    BeginToken(State.CODE),
                    *kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT).toTypedArray(),
                    EndToken
                )
            }
            possibleWhitespace()
            nodeOfType(KtTokens.RPAR)
            possibleWhitespace()
            nodeOfType(KtNodeTypes.THEN) andThen { nodes ->
                listOf(
                    LeafNodeToken(") "),
                    *kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT).toTypedArray()
                )
            }
        }
        zeroOrOne {
            possibleWhitespace()
            nodeOfType(KtTokens.ELSE_KEYWORD)
            possibleWhitespace()
            nodeOfType(KtNodeTypes.ELSE) andThen { nodes ->
                listOf(
                    WhitespaceToken(" "),
                    LeafNodeToken("else "),
                    *kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT).toTypedArray()
                )
            }
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(nodePattern.matchSequence(node.children().asIterable()), State.CODE)
}
