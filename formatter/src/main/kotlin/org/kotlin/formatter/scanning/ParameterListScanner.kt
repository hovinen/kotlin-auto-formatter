package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.inBeginEndBlock

/**
 * A [NodeScanner] for a list of parameters in a function or class declaration, or the arguments of
 * a function call expression.
 */
internal class ParameterListScanner(private val kotlinScanner: KotlinScanner): NodeScanner {
    private var isFirstEntry = false

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> {
        isFirstEntry = true
        val children = node.children().toList()
        return children.flatMap { scanEntry(it) }
    }

    private fun scanEntry(node: ASTNode): List<Token> {
        return when (node.elementType) {
            KtNodeTypes.VALUE_PARAMETER, KtNodeTypes.VALUE_ARGUMENT -> {
                val childTokens =
                    inBeginEndBlock(
                        kotlinScanner.scanNodes(node.children().asIterable(), ScannerState.STATEMENT),
                        State.CODE
                    )
                val breakToken =
                    SynchronizedBreakToken(whitespaceLength = if (isFirstEntry) 0 else 1)
                isFirstEntry = false
                listOf(breakToken).plus(childTokens)
            }
            KtTokens.WHITE_SPACE -> listOf()
            KtTokens.RPAR -> if (isFirstEntry) {
                listOf(LeafNodeToken(")"))
            } else {
                listOf(ClosingSynchronizedBreakToken(whitespaceLength = 0), LeafNodeToken(")"))
            }
            else -> kotlinScanner.scanInState(node, ScannerState.STATEMENT)
        }
    }
}
