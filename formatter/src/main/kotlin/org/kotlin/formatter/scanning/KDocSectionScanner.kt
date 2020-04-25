package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken

internal class KDocSectionScanner(private val kotlinScanner: KotlinScanner): NodeScanner {
    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> {
        val innerTokens =
            inBeginEndBlock(
                kotlinScanner.scanNodes(node.children().asIterable(), ScannerState.KDOC),
                State.LONG_COMMENT
            )
        return listOf(
            WhitespaceToken(length = lengthOfTokensForWhitespace(innerTokens), content = " "),
            *innerTokens.toTypedArray(),
            WhitespaceToken(length = 1, content = " ")
        )
    }
}
