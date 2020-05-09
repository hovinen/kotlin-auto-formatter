package org.kotlin.formatter.output

import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.BlockFromLastForcedBreakToken
import org.kotlin.formatter.ClosingForcedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import java.util.Stack

class TokenPreprocessor {
    private val resultStack = Stack<StackElement>()

    fun preprocess(input: List<Token>): List<Token> {
        resultStack.push(BlockStackElement(State.CODE))
        for (token in input) {
            when (token) {
                is WhitespaceToken -> {
                    if (token.content.isNotEmpty() || !(resultStack.peek() is WhitespaceStackElement)) {
                        resultStack.push(WhitespaceStackElement(token.content))
                    }
                }
                is BeginToken -> {
                    resultStack.push(BlockStackElement(token.state))
                }
                is EndToken -> {
                    val blockElement = popBlock()
                    val topElement = resultStack.peek()
                    topElement.tokens.add(BeginToken(length = blockElement.textLength, state = blockElement.state))
                    topElement.tokens.addAll(blockElement.tokens)
                    topElement.tokens.add(EndToken)
                }
                is BlockFromLastForcedBreakToken -> {
                    val topElement = popBlock()
                    val index = topElement.tokens.indexOfLast { it is ForcedBreakToken || it is ClosingForcedBreakToken }
                    val length = BlockStackElement(topElement.state, topElement.tokens.subList(index + 1, topElement.tokens.size)).textLength
                    topElement.tokens.add(index + 1, BeginToken(length = length, state = topElement.state))
                    topElement.tokens.add(EndToken)
                    resultStack.push(topElement)
                }
                else -> resultStack.peek().tokens.add(token)
            }
        }
        return popBlock().tokens
    }

    private fun popBlock(): BlockStackElement =
        when (val topElement = resultStack.pop()) {
            is BlockStackElement -> topElement
            is WhitespaceStackElement -> {
                val textLength = topElement.tokens.firstOrNull()?.textLength ?: 0
                resultStack.peek().tokens.add(
                    WhitespaceToken(length = textLength + topElement.contentLength, content = topElement.content)
                )
                resultStack.peek().tokens.addAll(topElement.tokens)
                popBlock()
            }
        }
}

private sealed class StackElement(internal val tokens: MutableList<Token> = mutableListOf()) {
    internal val textLength: Int get() =
        tokens.map {
            when (it) {
                is WhitespaceToken -> if (it.content.isEmpty()) 0 else 1
                is SynchronizedBreakToken -> it.whitespaceLength
                is LeafNodeToken -> it.textLength
                else -> 0
            }
        }.sum()
}

private class BlockStackElement(
    internal val state: State,
    tokens: MutableList<Token> = mutableListOf()
): StackElement(tokens)

private class WhitespaceStackElement(internal val content: String): StackElement() {
    internal val contentLength: Int = if (content.isEmpty()) 0 else 1
}

private val Token.textLength: Int
    get() =
        when(this) {
            is LeafNodeToken -> textLength
            is BeginToken -> length
            else -> 0
        }
