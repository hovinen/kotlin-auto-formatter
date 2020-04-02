package org.kotlin.formatter.output

import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.ClosingForcedBreakToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import java.util.Stack

class Printer(
    private val maxLineLength: Int,
    private val standardIndent: Int,
    private val continuationIndent: Int
) {
    private data class BlockStackEntry(
        val currentIndent: Int,
        val blockStartColumn: Int,
        private val token: BeginToken
    ) {
        fun topBlockFitsOnLine(maxLineLength: Int) = blockStartColumn + token.length <= maxLineLength

        val state = token.state
    }

    private val blockStack = Stack<BlockStackEntry>()
    private var spaceRemaining: Int = maxLineLength
    private var currentLineIndent: Int = 0
    private var result = StringBuilder()
    private val inStringLiteral get() = STRING_LITERAL_STATES.contains(blockStack.peek().state)

    fun print(tokens: List<Token>): String {
        blockStack.push(
            BlockStackEntry(
                currentIndent = 0,
                blockStartColumn = 0,
                token = BeginToken(length = 0, state = State.CODE)
            )
        )
        cleanUpWhitespace(tokens).forEach { printToken(it) }
        return result.toString()
    }

    private fun cleanUpWhitespace(tokens: List<Token>): List<Token> {
        val cleanedUpTokens = mutableListOf<Token>()
        var lastToken: Token? = null
        for (token in tokens) {
            when (token) {
                is WhitespaceToken -> {
                    lastToken = token
                }
                is ForcedBreakToken,
                is SynchronizedBreakToken,
                is ClosingForcedBreakToken,
                is ClosingSynchronizedBreakToken -> {
                    cleanedUpTokens.add(token)
                    lastToken = null
                }
                else -> {
                    if (lastToken != null) {
                        cleanedUpTokens.add(lastToken)
                    }
                    cleanedUpTokens.add(token)
                    lastToken = null
                }
            }
        }
        return cleanedUpTokens
    }

    private fun printToken(token: Token) {
        when (token) {
            is LeafNodeToken -> {
                result.append(token.text)
                spaceRemaining -= token.textLength
            }
            is WhitespaceToken -> {
                if (whitespacePlusFollowingTokenFitOnLine(token)) {
                    if (inStringLiteral) {
                        result.append(token.content)
                    } else {
                        result.append(" ")
                    }
                    spaceRemaining--
                } else {
                    val whitespaceFitsOnFirstLine =
                        spaceRemaining >= "${token.content}$STRING_BREAK_TERMINATOR".length
                    if (inStringLiteral && whitespaceFitsOnFirstLine) {
                        result.append(token.content)
                    }
                    indent(continuationIndent)
                    if (inStringLiteral && !whitespaceFitsOnFirstLine) {
                        result.append(token.content)
                    }
                }
            }
            is ForcedBreakToken -> {
                result.append("\n".repeat(token.count - 1))
                indent(standardIndent)
            }
            is ClosingForcedBreakToken -> {
                indent(0)
            }
            is SynchronizedBreakToken -> {
                if (!blockStack.peek().topBlockFitsOnLine(maxLineLength)) {
                    indent(continuationIndent)
                } else {
                    result.append(" ".repeat(token.whitespaceLength))
                }
            }
            is ClosingSynchronizedBreakToken -> {
                if (!blockStack.peek().topBlockFitsOnLine(maxLineLength)) {
                    if (blockStack.peek().state == State.LONG_COMMENT) {
                        indentForComment(" ")
                    } else {
                        indent(0)
                    }
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

    private fun whitespacePlusFollowingTokenFitOnLine(token: WhitespaceToken) =
        when (blockStack.peek().state) {
            State.MULTILINE_STRING_LITERAL -> true
            State.STRING_LITERAL -> token.length + STRING_BREAK_TERMINATOR.length <= spaceRemaining
            else -> token.length <= spaceRemaining
        }

    private fun indent(amount: Int) {
        when (blockStack.peek().state) {
            State.CODE -> {
                indentCode(amount)
            }
            State.LINE_COMMENT -> {
                indentForComment("// ")
            }
            State.TODO_COMMENT -> {
                indentForComment("//  ")
            }
            State.LONG_COMMENT -> {
                indentForComment(" * ")
            }
            State.KDOC_DIRECTIVE -> {
                indentForComment(" *     ")
            }
            State.STRING_LITERAL -> {
                result.append(STRING_BREAK_TERMINATOR)
                indentCode(amount)
                result.append('"')
            }
            else -> throw IllegalStateException(
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
        private val STRING_LITERAL_STATES = setOf(State.STRING_LITERAL, State.MULTILINE_STRING_LITERAL)
    }
}
