package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.scanning.nodepattern.NodePattern
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for `if` expressions. */
internal class IfExpressionScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern =
        nodePattern {
            exactlyOne {
                nodeOfType(KtTokens.IF_KEYWORD)
                possibleWhitespace()
                nodeOfType(KtTokens.LPAR)
                nodeOfType(KtNodeTypes.CONDITION) thenMapToTokens { nodes ->
                    listOf(LeafNodeToken("if ("), BeginToken(State.CODE))
                        .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
                        .plus(EndToken)
                }
                possibleWhitespace()
                nodeOfType(KtTokens.RPAR)
                possibleWhitespace()
                nodeOfType(KtNodeTypes.THEN) thenMapToTokens { nodes ->
                    thenPattern.matchSequence(nodes.first().children().asIterable())
                }
            }
            zeroOrOne {
                possibleWhitespace()
                nodeOfType(KtTokens.ELSE_KEYWORD)
                possibleWhitespace()
                nodeOfType(KtNodeTypes.ELSE) thenMapToTokens { nodes ->
                    elsePattern.matchSequence(nodes.first().children().asIterable())
                }
            }
            end()
        }

    private val thenPattern: NodePattern =
        nodePattern {
            either {
                nodeOfType(KtNodeTypes.BLOCK) thenMapToTokens { nodes ->
                    listOf(LeafNodeToken(") "))
                        .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
                }
            } or {
                oneOrMore { anyNode() } thenMapToTokens { nodes ->
                    listOf(LeafNodeToken(")"), SynchronizedBreakToken(whitespaceLength = 1))
                        .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
                }
            }
            end()
        }

    private val elsePattern =
        nodePattern {
            either {
                nodeOfOneOfTypes(KtNodeTypes.BLOCK, KtNodeTypes.IF) thenMapToTokens { nodes ->
                    listOf(WhitespaceToken(" "), LeafNodeToken("else "))
                        .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
                }
            } or {
                oneOrMore { anyNode() } thenMapToTokens { nodes ->
                    listOf(
                        ClosingSynchronizedBreakToken(whitespaceLength = 1),
                        LeafNodeToken("else"),
                        SynchronizedBreakToken(whitespaceLength = 1)
                    ).plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
                }
            }
            end()
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(nodePattern.matchSequence(node.children().asIterable()), State.CODE)
}
