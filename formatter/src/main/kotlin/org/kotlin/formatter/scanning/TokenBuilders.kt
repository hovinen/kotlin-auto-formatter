package org.kotlin.formatter.scanning

import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken

/**
 * Returns a sequence of [Token] which wraps [innerTokens] inside a [BeginToken], [EndToken] pair
 * with the given [State].
 */
internal fun inBeginEndBlock(innerTokens: List<Token>, state: State): List<Token> =
    listOf(
        BeginToken(state),
        *innerTokens.toTypedArray(),
        EndToken
    )

/**
 * Returns a [Token] which inserts a point where a line break may be introduced but produces no
 * output if no line break occurs.
 */
internal fun emptyBreakPoint() = WhitespaceToken("")
