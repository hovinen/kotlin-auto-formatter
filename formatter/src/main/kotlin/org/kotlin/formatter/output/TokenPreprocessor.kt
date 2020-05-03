package org.kotlin.formatter.output

import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.EndToken
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
                    resultStack.push(WhitespaceStackElement(token.content))
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
                    WhitespaceToken(length = textLength + 1, content = topElement.content)
                )
                resultStack.peek().tokens.addAll(topElement.tokens)
                popBlock()
            }
        }
}

private sealed class StackElement {
    internal val tokens = mutableListOf<Token>()

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

private class BlockStackElement(internal val state: State): StackElement()

private class WhitespaceStackElement(internal val content: String): StackElement()

private val Token.textLength: Int
    get() =
        when(this) {
            is LeafNodeToken -> textLength
            is BeginToken -> length
            else -> 0
        }
