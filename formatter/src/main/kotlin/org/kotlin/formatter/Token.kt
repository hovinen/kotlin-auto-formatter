package org.kotlin.formatter

sealed class Token

data class LeafNodeToken(internal val text: String) : Token() {
    internal val textLength: Int = text.length

    override fun toString(): String {
        return "LeafNodeToken(text=${text})"
    }
}

fun nonBreakingSpaceToken(content: String = " "): Token = LeafNodeToken(text = content)

data class WhitespaceToken(internal val content: String, internal val length: Int = 0) : Token()

data class ForcedBreakToken(internal val count: Int) : Token()

object ClosingForcedBreakToken : Token()

data class SynchronizedBreakToken(internal val whitespaceLength: Int) : Token()

data class ClosingSynchronizedBreakToken(internal val whitespaceLength: Int) : Token()

data class BeginToken(internal val state: State, internal val length: Int = 0) : Token()

object EndToken : Token()
