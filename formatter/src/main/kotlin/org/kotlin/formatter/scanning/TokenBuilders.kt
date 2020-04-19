package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.*

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

