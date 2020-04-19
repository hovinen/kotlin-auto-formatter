package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.lexer.KtTokens
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken

internal class LeafScanner {
    fun scanLeaf(node: LeafPsiElement, scannerState: ScannerState): List<Token> =
        when (node.elementType) {
            KtTokens.EOL_COMMENT -> {
                listOf(
                    BeginToken(node.textLength, stateBasedOnCommentContent(node.text)),
                    *tokenizeString(node.text).toTypedArray(),
                    EndToken
                )
            }
            KtTokens.BLOCK_COMMENT -> {
                listOf(
                    BeginToken(node.textLength, State.LONG_COMMENT),
                    *tokenizeNodeContentInBlockComment(node).toTypedArray(),
                    EndToken
                )
            }
            KDocTokens.TEXT -> tokenizeString(node.text.trim())
            KDocTokens.LEADING_ASTERISK -> listOf()
            KDocTokens.END -> listOf(ClosingSynchronizedBreakToken(whitespaceLength = 1), LeafNodeToken(node.text))
            KtTokens.REGULAR_STRING_PART -> tokenizeString(node.text)
            KtTokens.RPAR -> {
                if (scannerState == ScannerState.SYNC_BREAK_LIST) {
                    listOf(ClosingSynchronizedBreakToken(whitespaceLength = 0), LeafNodeToken(node.text))
                } else {
                    listOf(LeafNodeToken(node.text))
                }
            }
            KtTokens.DOT, KtTokens.SAFE_ACCESS -> {
                listOf(
                    SynchronizedBreakToken(whitespaceLength = 0),
                    LeafNodeToken(node.text)
                )
            }
            else -> listOf(LeafNodeToken(node.text))
        }

    private fun stateBasedOnCommentContent(content: String) =
        if (content.startsWith("// TODO")) State.TODO_COMMENT else State.LINE_COMMENT

    private fun tokenizeString(text: String): List<Token> {
        val parts = text.split(" ")
        return listOf(
            LeafNodeToken(parts.first()),
            *parts.tail().flatMap {
                listOf(
                    WhitespaceToken(it.length + 1, " "),
                    LeafNodeToken(it)
                )
            }.toTypedArray()
        )
    }

    private fun tokenizeNodeContentInBlockComment(node: ASTNode): List<Token> =
        tokenizeString(node.text.replace(Regex("\n[ ]+\\* "), "\n "))
}
