package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.State
import org.kotlin.formatter.Token

internal class SimpleBlockScanner(private val kotlinScanner: KotlinScanner, private val scannerState: ScannerState, private val state: State): NodeScanner {
    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(kotlinScanner.scanNodes(node.children().asIterable(), this.scannerState), state)
}
