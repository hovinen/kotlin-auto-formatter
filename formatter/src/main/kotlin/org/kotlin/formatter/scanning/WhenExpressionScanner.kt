package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.ClosingForcedBreakToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for `when` expressions. */
internal class WhenExpressionScanner(private val kotlinScanner: KotlinScanner): NodeScanner {
    private val nodePattern = nodePattern {
        nodeOfType(KtTokens.WHEN_KEYWORD) andThen { listOf(LeafNodeToken("when ")) }
        possibleWhitespace()
        zeroOrOne {
            nodeOfType(KtTokens.LPAR)
            possibleWhitespace()
            anyNode() andThen { nodes ->
                listOf(
                    BeginToken(State.CODE),
                    LeafNodeToken("("),
                    *kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT).toTypedArray(),
                    ClosingSynchronizedBreakToken(whitespaceLength = 0),
                    LeafNodeToken(") ")
                )
            }
            possibleWhitespace()
            nodeOfType(KtTokens.RPAR)
        }
        possibleWhitespace()
        nodeOfType(KtTokens.LBRACE) andThen {
            listOf(
                LeafNodeToken("{"),
                EndToken
            )
        }
        possibleWhitespace()
        zeroOrMore {
            nodeOfType(KtNodeTypes.WHEN_ENTRY) andThen { nodes ->
                listOf(
                    ForcedBreakToken(count = 1),
                    *kotlinScanner.scanNodes(nodes, ScannerState.BLOCK).toTypedArray()
                )
            }
            possibleWhitespace()
        }
        nodeOfType(KtTokens.RBRACE) andThen {
            listOf(
                ClosingForcedBreakToken,
                LeafNodeToken("}")
            )
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(nodePattern.matchSequence(node.children().asIterable()), State.CODE)
}