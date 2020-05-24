package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.emptyBreakPoint
import org.kotlin.formatter.inBeginEndBlock

/** A [NodeScanner] for string templates. */
internal class StringLiteralScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> {
        val tokens = mutableListOf<Token>()
        var lastChild: ASTNode? = null
        for (child in node.children()) {
            val childTokens = kotlinScanner.scanInState(child, ScannerState.BLOCK)
            if (child.elementType != KtTokens.OPEN_QUOTE &&
                child.elementType != KtTokens.CLOSING_QUOTE &&
                lastChild?.elementType != KtTokens.OPEN_QUOTE
            ) {
                tokens.add(emptyBreakPoint())
            }
            tokens.addAll(childTokens)
            lastChild = child
        }
        return inBeginEndBlock(tokens, stateForStringLiteral(node))
    }

    private fun stateForStringLiteral(node: ASTNode): State =
        if (node.text.startsWith("\"\"\"")) {
            State.MULTILINE_STRING_LITERAL
        } else {
            State.STRING_LITERAL
        }
}
