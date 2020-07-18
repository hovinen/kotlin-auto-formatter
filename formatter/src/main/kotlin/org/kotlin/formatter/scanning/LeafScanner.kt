package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.lexer.KtTokens
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.LiteralWhitespaceToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token

/** Scans leaf nodes in the Kotlin abstract syntax tree. */
internal class LeafScanner {
    /** Scans the given [LeafPsiElement], returning the corresponding list of [Token]. */
    internal fun scanLeaf(node: LeafPsiElement): List<Token> =
        when (node.elementType) {
            KDocTokens.END -> listOf(LeafNodeToken(node.text))
            KtTokens.REGULAR_STRING_PART -> tokenizeString(node.text) { LiteralWhitespaceToken(it) }
            else -> listOf(LeafNodeToken(node.text))
        }

    /**
     * Scans the given [ASTNode] representing a comment, returning the corresponding list of
     * [Token].
     */
    internal fun scanCommentNode(node: ASTNode): List<Token> =
        when (node.elementType) {
            KtTokens.EOL_COMMENT ->
                listOf(BeginToken(stateBasedOnCommentContent(node.text)))
                    .plus(LeafScanner().tokenizeString(node.text) { LiteralWhitespaceToken(it) })
                    .plus(EndToken)
            KtTokens.BLOCK_COMMENT ->
                listOf(
                    BeginToken(State.LONG_COMMENT),
                    LeafNodeToken("/*"),
                    LiteralWhitespaceToken(" ")
                ).plus(tokenizeNodeContentInBlockComment(node))
                    .plus(LeafNodeToken(" */"))
                    .plus(EndToken)
            else -> throw IllegalArgumentException("Invalid node for comment $node")
        }

    private fun tokenizeNodeContentInBlockComment(node: ASTNode): List<Token> {
        val text =
            node.text
                .removePrefix("/*")
                .removeSuffix("*/")
                .replace(Regex("\n[ ]+\\* "), "\n ")
                .trim()
        return tokenizeString(text) { LiteralWhitespaceToken(it) }
    }

    private fun stateBasedOnCommentContent(content: String) =
        if (content.startsWith("// TODO")) State.TODO_COMMENT else State.LINE_COMMENT

    private fun tokenizeString(text: String, whitespaceTokenSupplier: (String) -> Token):
        List<Token> {
            val whitespaceRegex = Regex("\\s+")
            var match = whitespaceRegex.find(text)
            val result = mutableListOf<Token>()
            var start = 0
            while (match != null) {
                result.add(LeafNodeToken(text.substring(start, match.range.first)))
                result.add(whitespaceTokenSupplier(match.value))
                start = match.range.last + 1
                match = match.next()
            }
            if (start < text.length) {
                result.add(LeafNodeToken(text.substring(start)))
            }
            return result
        }
}
