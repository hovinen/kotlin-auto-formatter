package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken

internal class KDocSectionScanner(private val kotlinScanner: KotlinScanner) {
    fun scanKDocSection(node: ASTNode): List<Token> {
        val innerTokens = kotlinScanner.tokensForBlockNode(node, State.LONG_COMMENT, ScannerState.KDOC)
        return listOf(
            WhitespaceToken(length = lengthOfTokensForWhitespace(innerTokens), content = " "),
            *innerTokens.toTypedArray(),
            WhitespaceToken(length = 1, content = " ")
        )
    }
}
