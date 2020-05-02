package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.scanning.nodepattern.nodePattern

internal class DotQualifiedExpressionScanner(private val kotlinScanner: KotlinScanner): NodeScanner {
    private val singleDotExpression = nodePattern {
        anyNode() andThen { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
        }
        possibleWhitespace()
        nodeOfOneOfTypes(KtTokens.DOT, KtTokens.SAFE_ACCESS) andThen { nodes ->
            listOf(LeafNodeToken(nodes.first().text))
        }
        possibleWhitespace()
        oneOrMore { anyNode() } andThen { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> {
        if (dotExpressionTypes.contains(node.firstChildNode.elementType)) {
            val tokens = scanInnerDotQualifiedExpression(node)
            return inBeginEndBlock(tokens, stateForDotQualifiedExpression(scannerState))
        } else {
            return inBeginEndBlock(
                singleDotExpression.matchSequence(node.children().asIterable()),
                stateForDotQualifiedExpression(scannerState)
            )
        }
    }

    private fun scanInnerDotQualifiedExpression(node: ASTNode): List<Token> {
        return if (dotExpressionTypes.contains(node.firstChildNode.elementType)) {
            listOf(
                *scanInnerDotQualifiedExpression(node.firstChildNode).toTypedArray(),
                *kotlinScanner.scanNodes(node.children().toList().tail(), ScannerState.STATEMENT).toTypedArray()
            )
        } else {
            kotlinScanner.scanNodes(node.children().asIterable(), ScannerState.STATEMENT)
        }
    }

    private fun stateForDotQualifiedExpression(scannerState: ScannerState) =
        if (scannerState == ScannerState.PACKAGE_IMPORT) State.PACKAGE_IMPORT else State.CODE

    companion object {
        private val dotExpressionTypes =
            setOf(KtNodeTypes.DOT_QUALIFIED_EXPRESSION, KtNodeTypes.SAFE_ACCESS_EXPRESSION)
    }
}
