package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for member access and safe member access expressions. */
internal class DotQualifiedExpressionScanner(private val kotlinScanner: KotlinScanner): NodeScanner {
    private val singleDotExpression = nodePattern {
        either {
            nodeOfOneOfTypes(KtNodeTypes.DOT_QUALIFIED_EXPRESSION, KtNodeTypes.SAFE_ACCESS_EXPRESSION) andThen { nodes ->
                scanInner(nodes[0])
            }
            possibleWhitespace()
            nodeOfOneOfTypes(KtTokens.DOT, KtTokens.SAFE_ACCESS) andThen { nodes ->
                listOf(
                    SynchronizedBreakToken(whitespaceLength = 0),
                    LeafNodeToken(nodes.first().text)
                )
            }
        } or {
            anyNode() andThen { nodes ->
                kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
            }
            possibleWhitespace()
            nodeOfOneOfTypes(KtTokens.DOT, KtTokens.SAFE_ACCESS) andThen { nodes ->
                listOf(LeafNodeToken(nodes.first().text))
            }
        }
        possibleWhitespace()
        oneOrMore { anyNode() } andThen { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(
            singleDotExpression.matchSequence(node.children().asIterable()),
            stateForDotQualifiedExpression(scannerState)
        )

    fun scanInner(node: ASTNode): List<Token> =
        singleDotExpression.matchSequence(node.children().asIterable())

    private fun stateForDotQualifiedExpression(scannerState: ScannerState) =
        if (scannerState == ScannerState.PACKAGE_IMPORT) State.PACKAGE_IMPORT else State.CODE
}
