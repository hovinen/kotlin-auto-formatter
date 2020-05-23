package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.Token

/** A [NodeScanner] for any [ASTNode] whose output consists of the scanned children. */
internal class SimpleScanner(
    private val kotlinScanner: KotlinScanner,
    private val scannerState: ScannerState
): NodeScanner {
    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        kotlinScanner.scanNodes(node.children().asIterable(), this.scannerState)
}
