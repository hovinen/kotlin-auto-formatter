package org.kotlin.formatter.output

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken

internal class TokenPreprocessorTest {
    @Test
    fun `outputs a LeafNodeToken directly`() {
        val subject = TokenPreprocessor()
        val input = listOf(
            LeafNodeToken("any token")
        )

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(input)
    }

    @ParameterizedTest
    @ValueSource(strings = ["any token", "another token"])
    fun `outputs a WhitespaceToken with the length of the following token`(token: String) {
        val subject = TokenPreprocessor()
        val input = listOf(
            WhitespaceToken(length = 0, content = " "),
            LeafNodeToken(token)
        )

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(
            listOf(
                WhitespaceToken(length = token.length + 1, content = " "),
                LeafNodeToken(token)
            )
        )
    }

    @Test
    fun `outputs a WhitespaceToken with the length of the following block`() {
        val subject = TokenPreprocessor()
        val input = listOf(
            WhitespaceToken(length = 0, content = " "),
            BeginToken(length = 0, state = State.CODE),
            LeafNodeToken("any token"),
            EndToken
        )

        val result = subject.preprocess(input)

        assertThat(result).contains(WhitespaceToken(length = 10, content = " "))
    }

    @Test
    fun `does not include the length of following tokens in the length of a WhitespaceToken`() {
        val subject = TokenPreprocessor()
        val input = listOf(
            WhitespaceToken(length = 0, content = " "),
            LeafNodeToken("any token"),
            LeafNodeToken("another token")
        )

        val result = subject.preprocess(input)

        assertThat(result).contains(WhitespaceToken(length = 10, content = " "))
    }

    @ParameterizedTest
    @MethodSource("tokenLengthCases")
    fun `outputs a BeginToken, EndToken pair with length`(token: Token, lengthExpected: Int) {
        val subject = TokenPreprocessor()
        val input = listOf(
            BeginToken(length = 0, state = State.CODE),
            token,
            EndToken
        )

        val result = subject.preprocess(input)

        assertThat(result).contains(BeginToken(length = lengthExpected, state = State.CODE))
    }

    @Test
    fun `outputs a BeginToken, EndToken pair with length of two tokens`() {
        val subject = TokenPreprocessor()
        val input = listOf(
            BeginToken(length = 0, state = State.CODE),
            LeafNodeToken("token 1"),
            LeafNodeToken("token 2"),
            EndToken
        )

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(
            listOf(
                BeginToken(length = 14, state = State.CODE),
                LeafNodeToken("token 1"),
                LeafNodeToken("token 2"),
                EndToken
            )
        )
    }

    @Test
    fun `outputs BeginToken using the state of the input token`() {
        val subject = TokenPreprocessor()
        val input = listOf(
            BeginToken(length = 0, state = State.STRING_LITERAL),
            LeafNodeToken("any token"),
            EndToken
        )

        val result = subject.preprocess(input)

        assertThat(result).contains(BeginToken(length = 9, state = State.STRING_LITERAL))
    }

    companion object {
        @JvmStatic
        fun tokenLengthCases(): List<Arguments> =
            listOf(
                Arguments.of(LeafNodeToken("any token"), 9),
                Arguments.of(WhitespaceToken(length = 0, content = "  "), 1),
                Arguments.of(WhitespaceToken(length = 0, content = ""), 0),
                Arguments.of(SynchronizedBreakToken(whitespaceLength = 2), 2)
            )
    }
}
