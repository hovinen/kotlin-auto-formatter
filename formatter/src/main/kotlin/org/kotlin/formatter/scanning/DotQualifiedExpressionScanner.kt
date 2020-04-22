package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.State
import org.kotlin.formatter.Token

internal class DotQualifiedExpressionScanner(private val kotlinScanner: KotlinScanner) {
    fun scanDotQualifiedExpression(node: ASTNode, scannerState: ScannerState): List<Token> {
        val tokens = scanInnerDotQualifiedExpression(node)
        return inBeginEndBlock(tokens, stateForDotQualifiedExpression(scannerState))
    }

    private fun scanInnerDotQualifiedExpression(node: ASTNode): List<Token> {
        val dotExpressionTypes =
            setOf(KtNodeTypes.DOT_QUALIFIED_EXPRESSION, KtNodeTypes.SAFE_ACCESS_EXPRESSION)
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
}
