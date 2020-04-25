package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token

internal class ConditionScanner(private val kotlinScanner: KotlinScanner): NodeScanner {
    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> {
        val innerTokens = kotlinScanner.scanNodes(node.children().asIterable(), ScannerState.STATEMENT)
        return listOf(
            BeginToken(length = lengthOfTokens(innerTokens), state = State.CODE),
            *innerTokens.toTypedArray(),
            ClosingSynchronizedBreakToken(whitespaceLength = 0),
            EndToken
        )
    }
}
