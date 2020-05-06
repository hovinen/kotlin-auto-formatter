package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.State
import org.kotlin.formatter.Token

internal class KDocSectionScanner(private val kotlinScanner: KotlinScanner): NodeScanner {
    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> {
        return inBeginEndBlock(
            kotlinScanner.scanNodes(node.children().asIterable(), ScannerState.KDOC),
            State.LONG_COMMENT
        )
    }
}
