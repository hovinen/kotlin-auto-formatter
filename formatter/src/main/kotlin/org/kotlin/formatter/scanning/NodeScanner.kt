package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.kotlin.formatter.Token

internal interface NodeScanner {
    fun scan(node: ASTNode, scannerState: ScannerState): List<Token>
}
