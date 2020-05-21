package org.kotlin.formatter.output

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.BlockFromMarkerToken
import org.kotlin.formatter.ClosingForcedBreakToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.KDocContentToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.MarkerToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.emptyBreakPoint

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
    @MethodSource("whitespaceTokenLengthCases")
    fun `outputs a WhitespaceToken with the length of the following token`(token: Token, lengthExpected: Int) {
        val subject = TokenPreprocessor()
        val input = listOf(
            WhitespaceToken(length = 0, content = " "),
            token
        )

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(
            listOf(
                WhitespaceToken(length = lengthExpected, content = " "),
                token
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
    fun `consolidates WhitespaceToken with emptyBreakPoint`() {
        val subject = TokenPreprocessor()
        val input = listOf(
            WhitespaceToken(length = 0, content = " "),
            emptyBreakPoint(),
            LeafNodeToken("any token")
        )

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(
            listOf(
                WhitespaceToken(length = 10, content = " "),
                LeafNodeToken("any token")
            )
        )
    }

    @Test
    fun `preserves emptyBreakPoint when not immediately preceeded by a WhitespaceToken`() {
        val subject = TokenPreprocessor()
        val input = listOf(
            emptyBreakPoint(),
            LeafNodeToken("any token")
        )

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(
            listOf(
                WhitespaceToken(length = 9, content = ""),
                LeafNodeToken("any token")
            )
        )
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

    @Test
    fun `outputs BeginToken, EndToken pair from start of block for BlockFromMarkerToken`() {
        val subject = TokenPreprocessor()
        val input = listOf(
            LeafNodeToken("any token"),
            BlockFromMarkerToken
        )

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(
            listOf(
                BeginToken(length = 9, state = State.CODE),
                LeafNodeToken("any token"),
                EndToken
            )
        )
    }

    @Test
    fun `outputs BeginToken, EndToken pair from MarkerToken for BlockFromMarkerToken`() {
        val subject = TokenPreprocessor()
        val input = listOf(
            LeafNodeToken("any token"),
            MarkerToken,
            BlockFromMarkerToken
        )

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(
            listOf(
                LeafNodeToken("any token"),
                BeginToken(length = 0, state = State.CODE),
                EndToken
            )
        )
    }

    @Test
    fun `removes unmatched MarkerToken`() {
        val subject = TokenPreprocessor()
        val input = listOf(
            LeafNodeToken("any token"),
            MarkerToken
        )

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(
            listOf(
                LeafNodeToken("any token")
            )
        )
    }

    @Test
    fun `maintains tokens following dropped MarkerToken`() {
        val subject = TokenPreprocessor()
        val input = listOf(
            MarkerToken,
            LeafNodeToken("any token")
        )

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(
            listOf(
                LeafNodeToken("any token")
            )
        )
    }

    @Test
    fun `does not output BeginToken from MarkerToken in subblock`() {
        val subject = TokenPreprocessor()
        val input = listOf(
            LeafNodeToken("any token"),
            BeginToken(state = State.CODE),
            MarkerToken,
            EndToken,
            BlockFromMarkerToken
        )

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(
            listOf(
                BeginToken(length = 9, state = State.CODE),
                LeafNodeToken("any token"),
                BeginToken(state = State.CODE),
                EndToken,
                EndToken
            )
        )
    }

    @ParameterizedTest
    @MethodSource("synchronizedBreakTokenCases")
    fun `converts synchronized break into forced break when KDoc token with newline the same block`(
        synchronizedBreakToken: Token, expectedBreakToken: Token
    ) {
        val subject = TokenPreprocessor()
        val input = listOf(
            BeginToken(state = State.CODE),
            synchronizedBreakToken,
            KDocContentToken("\n"),
            EndToken
        )

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(
            listOf(
                BeginToken(length = 1, state = State.CODE),
                expectedBreakToken,
                KDocContentToken("\n"),
                EndToken
            )
        )
    }

    @ParameterizedTest
    @MethodSource("synchronizedBreakTokenCases")
    fun `does not convert synchronized break tokens in a subblock into forced break tokens`(
        synchronizedBreakToken: Token, unused: Token
    ) {
        val subject = TokenPreprocessor()
        val input = listOf(
            BeginToken(state = State.CODE),
            BeginToken(state = State.CODE),
            synchronizedBreakToken,
            EndToken,
            KDocContentToken("\n"),
            EndToken
        )

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(
            listOf(
                BeginToken(length = 1, state = State.CODE),
                BeginToken(length = 0, state = State.CODE),
                synchronizedBreakToken,
                EndToken,
                KDocContentToken("\n"),
                EndToken
            )
        )
    }

    companion object {
        @JvmStatic
        fun tokenLengthCases(): List<Arguments> =
            listOf(
                Arguments.of(LeafNodeToken("any token"), 9),
                Arguments.of(KDocContentToken("any content"), 11),
                Arguments.of(WhitespaceToken(length = 0, content = "  "), 1),
                Arguments.of(WhitespaceToken(length = 0, content = ""), 0),
                Arguments.of(SynchronizedBreakToken(whitespaceLength = 2), 2)
            )

        @JvmStatic
        fun whitespaceTokenLengthCases(): List<Arguments> =
            listOf(
                Arguments.of(LeafNodeToken("any token"), 10),
                Arguments.of(KDocContentToken("any content"), 12)
            )

        @JvmStatic
        fun synchronizedBreakTokenCases(): List<Arguments> =
            listOf(
                Arguments.of(SynchronizedBreakToken(whitespaceLength = 0), ForcedBreakToken(count = 1)),
                Arguments.of(ClosingSynchronizedBreakToken(whitespaceLength = 0), ClosingForcedBreakToken),
                Arguments.of(SynchronizedBreakToken(whitespaceLength = 0), ForcedBreakToken(count = 1)),
                Arguments.of(ClosingSynchronizedBreakToken(whitespaceLength = 0), ClosingForcedBreakToken)
            )
    }
}
