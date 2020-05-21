package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.nonBreakingSpaceToken
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for anonymous function literals, i.e. lambda expressions. */
internal class FunctionLiteralScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern = nodePattern {
        nodeOfType(KtTokens.LBRACE) andThen { listOf(LeafNodeToken("{")) }
        possibleWhitespace()
        zeroOrOne {
            nodeOfType(KtNodeTypes.VALUE_PARAMETER_LIST) andThen { nodes ->
                listOf(nonBreakingSpaceToken())
                    .plus(
                        inBeginEndBlock(
                            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT),
                            State.CODE
                        )
                    )
            }
            possibleWhitespace()
            nodeOfType(KtTokens.ARROW) andThen {
                listOf(nonBreakingSpaceToken(), LeafNodeToken("->"))
            }
            possibleWhitespace()
        }
        zeroOrOne {
            nodeOfType(KtNodeTypes.BLOCK) andThen { nodes ->
                val tokens = kotlinScanner.scanNodes(nodes, ScannerState.BLOCK)
                if (tokens.isNotEmpty()) {
                    listOf(SynchronizedBreakToken(whitespaceLength = 1))
                        .plus(tokens)
                        .plus(ClosingSynchronizedBreakToken(whitespaceLength = 1))
                } else {
                    listOf()
                }
            }
        }
        possibleWhitespace()
        nodeOfType(KtTokens.RBRACE) andThen { listOf(LeafNodeToken("}")) }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
