package org.kotlin.formatter.output

import java.util.Stack
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.ClosingForcedBreakToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.KDocContentToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.LiteralWhitespaceToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken

/**
 * Outputs a sequence of [Token], applying line breaks to keep as much as possible within a
 * specified column limit.
 *
 * This implementation is based loosely on the "print" operation of the "Prettyprinting" algorithm
 * of Derek Oppen [1] with some inspiration from the
 * [Google Java formatter](https://github.com/google/google-java-format).
 *
 * [1] Oppen, Derek C. "Prettyprinting". ACM Transactions on Programming Languages and Systems,
 * Volume 2 Issue 4, Oct. 1980, pp. 465-483.
 *
 * @property maxLineLength the column limit to which output source code should be held as much as
 *     possible. The [Google Kotlin style guide](https://developer.android.com/kotlin/style-guide)
 *     specifies 100 for this value.
 * @property standardIndent the amount in spaces by which to indent blocks, such as class and method
 *     content. The [Kotlin coding
 *     conventions](https://kotlinlang.org/docs/reference/coding-conventions.html) specify 4 for
 *     this value.
 * @property continuationIndent the amount in spaces by which to indent when inserting a line break
 *     within statement. The [Kotlin coding
 *     conventions](https://kotlinlang.org/docs/reference/coding-conventions.html) specify 4 for
 *     this value.
 */
class Printer(
    private val maxLineLength: Int,
    private val standardIndent: Int,
    private val continuationIndent: Int
) {
    private data class BlockStackEntry(
        val currentIndent: Int,
        val blockStartColumn: Int,
        private val token: BeginToken,
        val isInitial: Boolean = false
    ) {
        fun topBlockFitsOnLine(maxLineLength: Int) =
            blockStartColumn + token.length <= maxLineLength

        val state = token.state
    }

    private val blockStack = Stack<BlockStackEntry>()
    private var spaceRemaining: Int = maxLineLength
    private var currentLineIndent: Int = 0
    private var result = StringBuilder()
    private val inStringLiteral: Boolean
        get() = STRING_LITERAL_STATES.contains(blockStack.peek().state)
    private val inComment: Boolean
        get() = COMMENT_STATES.contains(blockStack.peek().state)
    private var atStartOfLine = true

    /**
     * Converts the given [tokens] into a formatted String.
     *
     * This method outputs the tokens in sequence, with [WhitespaceToken] converted into line breaks
     * at points where the whitespace content plus the following token would no longer fit on one
     * line. Extra whitespace and non-essential breaks, represented by [WhitespaceToken] whose
     * content contains a newline character, are consolidated to a single space. Trailing whitespace
     * is trimmed from a line.
     *
     * Forced line breaks which occur at the highest level of syntax do not result in indentation.
     * All other line breaks result in indentation either by [standardIndent] (for forced breaks) or
     * [continuationIndent] (for inserted breaks).
     *
     * Line breaks inside single-line string literals and long comments are treated properly. The
     * string literal is broken with a concatenation operator, while an initial asterisk is inserted
     * at the start of each line of a long comment. This behaviour is governed by the [State] of the
     * current syntactic block as given by the most recent [BeginToken].
     *
     * This method does not support [org.kotlin.formatter.BlockFromMarkerToken] or
     * [org.kotlin.formatter.MarkerToken] and ignores any such tokens encountered. The
     * [TokenPreprocessor] replaces any such tokens with tokens supported by this class.
     *
     * See the documentation of [Token] and its subclasses for more information on the printing
     * behaviour of each token.
     */
    fun print(tokens: List<Token>): String {
        blockStack.push(
            BlockStackEntry(
                currentIndent = 0,
                blockStartColumn = 0,
                token = BeginToken(length = 0, state = State.CODE),
                isInitial = true
            )
        )
        cleanUpWhitespace(tokens).forEach { printToken(it) }
        return result.toString()
    }

    private fun cleanUpWhitespace(tokens: List<Token>): List<Token> {
        val cleanedUpTokens = mutableListOf<Token>()
        var lastToken: WhitespaceToken? = null
        for (token in tokens) {
            when (token) {
                is WhitespaceToken -> {
                    lastToken = token
                }
                is ForcedBreakToken, is ClosingForcedBreakToken -> {
                    cleanedUpTokens.add(token)
                    lastToken = null
                }
                is SynchronizedBreakToken -> {
                    if (lastToken != null) {
                        cleanedUpTokens.add(
                            SynchronizedBreakToken(
                                whitespaceLength = lastToken.content.length + token.whitespaceLength
                            )
                        )
                        lastToken = null
                    } else {
                        cleanedUpTokens.add(token)
                    }
                }
                is ClosingSynchronizedBreakToken -> {
                    if (lastToken != null) {
                        cleanedUpTokens.add(
                            ClosingSynchronizedBreakToken(
                                whitespaceLength = lastToken.content.length + token.whitespaceLength
                            )
                        )
                        lastToken = null
                    } else {
                        cleanedUpTokens.add(token)
                    }
                }
                is EndToken -> {
                    cleanedUpTokens.add(token)
                }
                else -> {
                    if (lastToken != null) {
                        cleanedUpTokens.add(lastToken)
                        lastToken = null
                    }
                    cleanedUpTokens.add(token)
                }
            }
        }
        return cleanedUpTokens
    }

    private fun printToken(token: Token) {
        when (token) {
            is LeafNodeToken -> {
                appendTextOnSameLine(token.text)
                if (token.text.isNotEmpty()) {
                    atStartOfLine = false
                }
            }
            is KDocContentToken -> {
                appendKDocContent(token.content)
            }
            is WhitespaceToken -> {
                appendWhitespaceToken(token)
            }
            is LiteralWhitespaceToken -> {
                appendLiteralWhitespaceToken(token)
            }
            is ForcedBreakToken -> {
                if (inComment) {
                    for (i in 0 until token.count - 1) {
                        writeLineStartMarker()
                    }
                    indent(if (blockStack.peek().isInitial) 0 else standardIndent)
                } else {
                    result.append("\n".repeat(token.count - 1))
                    indent(if (blockStack.peek().isInitial) 0 else standardIndent)
                }
                atStartOfLine = true
            }
            is ClosingForcedBreakToken -> {
                if (inComment) {
                    indentCode(1)
                } else {
                    indent(0)
                }
                atStartOfLine = true
            }
            is SynchronizedBreakToken -> {
                if (breakingAllowed && !blockStack.peek().topBlockFitsOnLine(maxLineLength)) {
                    indent(continuationIndent)
                    atStartOfLine = true
                } else {
                    appendTextOnSameLine(" ".repeat(token.whitespaceLength))
                }
            }
            is ClosingSynchronizedBreakToken -> {
                if (!blockStack.peek().topBlockFitsOnLine(maxLineLength)) {
                    if (blockStack.peek().state == State.LONG_COMMENT) {
                        indentForComment(" ")
                    } else {
                        indent(0)
                    }
                    atStartOfLine = true
                } else {
                    appendTextOnSameLine(" ".repeat(token.whitespaceLength))
                }
            }
            is BeginToken -> {
                blockStack.push(
                    BlockStackEntry(
                        currentIndent = currentLineIndent,
                        blockStartColumn = maxLineLength - spaceRemaining,
                        token = token
                    )
                )
            }
            is EndToken -> {
                blockStack.pop()
            }
        }
    }

    private fun appendKDocContent(content: String) {
        val lines =
            KDocFormatter(maxLineLength = spaceRemaining - " * ".length).format(content).split("\n")
        if (atStartOfLine) {
            appendTextOnSameLine(if (lines.first().isNotBlank()) " * " else " *")
        }
        appendTextOnSameLine(lines.first())
        for (line in lines.tail()) {
            indentForComment(if (line.isNotBlank()) " * " else " *")
            appendTextOnSameLine(line)
        }
    }

    private fun <E> List<E>.tail(): List<E> = subList(1, size)

    private fun appendWhitespaceToken(token: WhitespaceToken) {
        if (!breakingAllowed || whitespacePlusFollowingTokenFitOnLine(token)) {
            if (token.content.isNotEmpty()) {
                appendTextOnSameLine(" ")
            }
        } else if (!atStartOfLine) {
            indent(continuationIndent)
        }
    }

    private fun appendLiteralWhitespaceToken(token: LiteralWhitespaceToken) {
        if (!breakingAllowed || token.length <= spaceRemaining) {
            if (inComment && token.content.contains('\n')) {
                appendWhitespaceForComment(token.content)
            } else {
                appendTextWithPossibleNewlines(token.content)
            }
        } else {
            val whitespaceFitsOnFirstLine =
                spaceRemaining >= "${token.content}$STRING_BREAK_TERMINATOR".length
            if (inStringLiteral && whitespaceFitsOnFirstLine) {
                appendTextOnSameLine(token.content)
            }
            indent(continuationIndent)
            if (inStringLiteral && !whitespaceFitsOnFirstLine) {
                appendTextOnSameLine(token.content)
            }
            if (inComment) {
                appendTextOnSameLine(" ")
            }
        }
    }

    private fun appendWhitespaceForComment(content: String) {
        val parts = content.split(Regex("\n"), 2)
        if (parts.size == 1) {
            appendTextOnSameLine(content)
        } else {
            appendTextOnSameLine(parts[0])
            indent(0)
            appendWhitespaceForComment(parts[1])
        }
    }

    private fun appendTextWithPossibleNewlines(text: String) {
        val newlineIndex = text.lastIndexOf('\n')
        if (newlineIndex == -1) {
            appendTextOnSameLine(text)
        } else {
            appendTextOnSameLine(text.substring(0, newlineIndex))
            result.append("\n")
            spaceRemaining = maxLineLength
            appendTextOnSameLine(text.substring(newlineIndex + 1))
        }
    }

    private fun appendTextOnSameLine(text: String) {
        result.append(text)
        spaceRemaining -= text.length
    }

    private val breakingAllowed: Boolean
        get() = !LINE_BREAK_SUPPRESSING_STATES.contains(blockStack.peek().state)

    private fun whitespacePlusFollowingTokenFitOnLine(token: WhitespaceToken) =
        when (blockStack.peek().state) {
            State.MULTILINE_STRING_LITERAL -> true
            else -> token.length <= spaceRemaining
        }

    private fun writeLineStartMarker() {
        when (blockStack.peek().state) {
            State.LINE_COMMENT, State.TODO_COMMENT -> {
                indentForComment("//")
            }
            State.LONG_COMMENT, State.KDOC_TAG -> {
                indentForComment(" *")
            }
            else -> {
                result.append("\n")
            }
        }
    }

    private fun indent(amount: Int) {
        when (blockStack.peek().state) {
            State.CODE -> {
                indentCode(amount)
            }
            State.LINE_COMMENT -> {
                indentForComment("//")
            }
            State.TODO_COMMENT -> {
                indentForComment("// ")
            }
            State.LONG_COMMENT -> {
                indentForComment(" *")
            }
            State.KDOC_TAG -> {
                indentForComment(" *    ")
            }
            State.STRING_LITERAL -> {
                result.append(STRING_BREAK_TERMINATOR)
                indentCode(amount)
                appendTextOnSameLine("\"")
            }
            State.PACKAGE_IMPORT -> {
                result.append("\n")
            }
            else ->
                throw IllegalStateException(
                    "Unrecognized state for line breaking ${blockStack.peek().state}"
                )
        }
    }

    private fun indentCode(amount: Int) {
        currentLineIndent = blockStack.peek().currentIndent + amount
        result.append("\n${" ".repeat(currentLineIndent)}")
        spaceRemaining = maxLineLength - currentLineIndent
    }

    private fun indentForComment(commentPrefix: String) {
        currentLineIndent = blockStack.peek().currentIndent
        result.append("\n${" ".repeat(currentLineIndent)}$commentPrefix")
        spaceRemaining = maxLineLength - currentLineIndent - commentPrefix.length
    }

    companion object {
        private const val STRING_BREAK_TERMINATOR = "\" +"
        internal const val STRING_BREAK_TERMINATOR_LENGTH = STRING_BREAK_TERMINATOR.length
        private val STRING_LITERAL_STATES =
            setOf(State.STRING_LITERAL, State.MULTILINE_STRING_LITERAL)
        private val COMMENT_STATES =
            setOf(State.LONG_COMMENT, State.KDOC_TAG, State.LINE_COMMENT, State.TODO_COMMENT)
        private val LINE_BREAK_SUPPRESSING_STATES =
            setOf(State.PACKAGE_IMPORT, State.MULTILINE_STRING_LITERAL)
    }
}
