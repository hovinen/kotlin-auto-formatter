package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.Token

/**
 * A [NodeScanner] for expressions in string templates.
 *
 * Does not output tokens which perform any formatting, but rather outputs a single [LeafNodeToken]
 * with the node text.
 */
internal class StringTemplateEntryScanner : NodeScanner {
    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        listOf(LeafNodeToken(node.text))
}
