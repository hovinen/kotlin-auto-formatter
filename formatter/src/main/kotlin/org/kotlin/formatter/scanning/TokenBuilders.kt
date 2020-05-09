package org.kotlin.formatter.scanning

import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken

internal fun inBeginEndBlock(innerTokens: List<Token>, state: State): List<Token> =
    listOf(
        BeginToken(state),
        *innerTokens.toTypedArray(),
        EndToken
    )

internal fun emptyBreakPoint() = WhitespaceToken("")

internal fun <T> List<T>.tail() = this.subList(1, this.size)
