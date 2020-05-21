package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token

/** A [NodeScanner] for the condition of `if` and `while` expressions. */
internal class ConditionScanner(private val kotlinScanner: KotlinScanner): NodeScanner {
    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        listOf(BeginToken(state = State.CODE))
            .plus(kotlinScanner.scanNodes(node.children().asIterable(), ScannerState.STATEMENT))
            .plus(ClosingSynchronizedBreakToken(whitespaceLength = 0))
            .plus(EndToken)
}
