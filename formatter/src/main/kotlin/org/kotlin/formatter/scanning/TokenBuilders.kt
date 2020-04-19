package org.kotlin.formatter.scanning

import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken

internal fun inBeginEndBlock(innerTokens: List<Token>, state: State): List<Token> =
    listOf(
        BeginToken(length = lengthOfTokens(innerTokens), state = state),
        *innerTokens.toTypedArray(),
        EndToken
    )

internal fun lengthOfTokens(nextTokens: List<Token>): Int =
    nextTokens.map {
        when (it) {
            is WhitespaceToken -> if (it.content.isEmpty()) 0 else 1
            is SynchronizedBreakToken -> it.whitespaceLength
            is LeafNodeToken -> it.textLength
            else -> 0
        }
    }.sum()

internal fun lengthOfTokensForWhitespace(nextTokens: List<Token>): Int =
    when (val firstToken = nextTokens.firstOrNull()) {
        is LeafNodeToken -> firstToken.textLength
        else -> lengthOfTokens(nextTokens)
    }

internal fun <T> List<T>.tail() = this.subList(1, this.size)
