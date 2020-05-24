package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.emptyBreakPoint
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for member access and safe member access expressions. */
internal class DotQualifiedExpressionScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(
            dotQualifiedExpressionPattern(true).matchSequence(node.children().asIterable()),
            stateForDotQualifiedExpression(scannerState)
        )

    private fun stateForDotQualifiedExpression(scannerState: ScannerState) =
        if (scannerState == ScannerState.PACKAGE_IMPORT) State.PACKAGE_IMPORT else State.CODE

    private fun scanInner(node: ASTNode): List<Token> =
        dotQualifiedExpressionPattern(false).matchSequence(node.children().asIterable())

    private fun dotQualifiedExpressionPattern(isOutermost: Boolean) =
        nodePattern {
            either {
                nodeOfOneOfTypes(
                    KtNodeTypes.DOT_QUALIFIED_EXPRESSION,
                    KtNodeTypes.SAFE_ACCESS_EXPRESSION
                ) thenMapToTokens { nodes -> scanInner(nodes.first()) }
                possibleWhitespace()
                nodeOfOneOfTypes(KtTokens.DOT, KtTokens.SAFE_ACCESS) thenMapToTokens { nodes ->
                    listOf(
                        SynchronizedBreakToken(whitespaceLength = 0),
                        BeginToken(State.CODE),
                        LeafNodeToken(nodes.first().text)
                    )
                }
            } or {
                anyNode() thenMapToTokens { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                }
                possibleWhitespace()
                nodeOfOneOfTypes(KtTokens.DOT, KtTokens.SAFE_ACCESS) thenMapToTokens { nodes ->
                    val tokens = listOf(BeginToken(State.CODE), LeafNodeToken(nodes.first().text))
                    if (isOutermost) tokens else listOf(emptyBreakPoint()).plus(tokens)
                }
            }
            possibleWhitespace()
            oneOrMore { anyNode() } thenMapToTokens { nodes ->
                kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT).plus(EndToken)
            }
            end()
        }
}
