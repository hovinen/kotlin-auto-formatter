package org.kotlin.formatter.output

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.BeginWeakToken
import org.kotlin.formatter.BlockFromMarkerToken
import org.kotlin.formatter.ClosingForcedBreakToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.ForceSynchronizedBreaksInBlockToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.KDocContentToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.LiteralWhitespaceToken
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
        val input = listOf(LeafNodeToken("any token"))

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(input)
    }

    @ParameterizedTest
    @MethodSource("whitespaceTokenLengthCases")
    fun `outputs a WhitespaceToken with the length of the following token`(
        token: Token,
        lengthExpected: Int
    ) {
        val subject = TokenPreprocessor()
        val input = listOf(WhitespaceToken(content = " "), token)

        val result = subject.preprocess(input)

        assertThat(result)
            .isEqualTo(listOf(WhitespaceToken(length = lengthExpected, content = " "), token))
    }

    @Test
    fun `outputs a WhitespaceToken with the length of the following sequence of leaf tokens`() {
        val subject = TokenPreprocessor()
        val input = listOf(WhitespaceToken(content = " "), LeafNodeToken("a"), LeafNodeToken("b"))

        val result = subject.preprocess(input)

        assertThat(result)
            .isEqualTo(
                listOf(
                    WhitespaceToken(length = 3, content = " "),
                    LeafNodeToken("a"),
                    LeafNodeToken("b")
                )
            )
    }

    @Test
    fun `outputs a WhitespaceToken with the length of the following block`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                WhitespaceToken(content = " "),
                BeginToken(state = State.CODE),
                LeafNodeToken("any token"),
                EndToken
            )

        val result = subject.preprocess(input)

        assertThat(result).contains(WhitespaceToken(length = 10, content = " "))
    }

    @Test
    fun `outputs a WhitespaceToken without the length of the following weak block`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                WhitespaceToken(content = " "),
                BeginWeakToken(),
                LeafNodeToken(""),
                LeafNodeToken("any token"),
                EndToken
            )

        val result = subject.preprocess(input)

        assertThat(result).contains(WhitespaceToken(length = 1, content = " "))
    }

    @Test
    fun `outputs a WhitespaceToken with the length of the following block inside a weak block`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                WhitespaceToken(content = " "),
                BeginWeakToken(),
                BeginToken(State.CODE),
                LeafNodeToken("any token"),
                EndToken,
                EndToken
            )

        val result = subject.preprocess(input)

        assertThat(result).contains(WhitespaceToken(length = 10, content = " "))
    }

    @Test
    fun `outputs a LiteralWhitespaceToken with the length of the following token`() {
        val subject = TokenPreprocessor()
        val input = listOf(LiteralWhitespaceToken(content = " "), LeafNodeToken("any token"))

        val result = subject.preprocess(input)

        assertThat(result).contains(LiteralWhitespaceToken(length = 10, content = " "))
    }

    @Test
    fun `correctly considers whitespace length in a string literal`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                BeginToken(state = State.STRING_LITERAL),
                LiteralWhitespaceToken(content = "  "),
                LeafNodeToken("any token"),
                EndToken
            )

        val result = subject.preprocess(input)

        assertThat(result).contains(BeginToken(length = 11, state = State.STRING_LITERAL))
    }

    @Test
    fun `correctly considers whitespace length in a nested string literal`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                BeginToken(state = State.CODE),
                BeginToken(state = State.STRING_LITERAL),
                LiteralWhitespaceToken(content = "  "),
                LeafNodeToken("any token"),
                EndToken,
                EndToken
            )

        val result = subject.preprocess(input)

        assertThat(result).contains(BeginToken(length = 11, state = State.CODE))
    }

    @Test
    fun `consolidates WhitespaceToken with emptyBreakPoint`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                WhitespaceToken(length = 0, content = " "),
                emptyBreakPoint(),
                LeafNodeToken("any token")
            )

        val result = subject.preprocess(input)

        assertThat(result)
            .isEqualTo(
                listOf(WhitespaceToken(length = 10, content = " "), LeafNodeToken("any token"))
            )
    }

    @Test
    fun `preserves emptyBreakPoint when not preceded by a WhitespaceToken`() {
        val subject = TokenPreprocessor()
        val input = listOf(emptyBreakPoint(), LeafNodeToken("any token"))

        val result = subject.preprocess(input)

        assertThat(result)
            .isEqualTo(
                listOf(WhitespaceToken(length = 9, content = ""), LeafNodeToken("any token"))
            )
    }

    @Test
    fun `preserves emptyBreakPoint when not immediately preceded by a WhitespaceToken`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                WhitespaceToken(content = " "),
                LeafNodeToken("any token"),
                emptyBreakPoint(),
                LeafNodeToken("any token")
            )

        val result = subject.preprocess(input)

        assertThat(result)
            .isEqualTo(
                listOf(
                    WhitespaceToken(length = 10, content = " "),
                    LeafNodeToken("any token"),
                    WhitespaceToken(length = 9, content = ""),
                    LeafNodeToken("any token")
                )
            )
    }

    @Test
    fun `consolidates SynchronizedBreakToken with WhitespaceToken`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                SynchronizedBreakToken(whitespaceLength = 1),
                WhitespaceToken(length = 0, content = " ")
            )

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(listOf(SynchronizedBreakToken(whitespaceLength = 1)))
    }

    @Test
    fun `consolidates ClosingSynchronizedBreakToken with WhitespaceToken`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                ClosingSynchronizedBreakToken(whitespaceLength = 1),
                WhitespaceToken(length = 0, content = " ")
            )

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(listOf(ClosingSynchronizedBreakToken(whitespaceLength = 1)))
    }

    @Test
    fun `adjust whitespace length of SynchronizedBreakToken followed by WhitespaceToken`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                SynchronizedBreakToken(whitespaceLength = 0),
                WhitespaceToken(length = 0, content = " ")
            )

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(listOf(SynchronizedBreakToken(whitespaceLength = 1)))
    }

    @Test
    fun `consolidates SynchronizedBreakToken with WhitespaceToken when MarkerToken is between`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                SynchronizedBreakToken(whitespaceLength = 1),
                MarkerToken,
                WhitespaceToken(length = 0, content = " ")
            )

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(listOf(SynchronizedBreakToken(whitespaceLength = 1)))
    }

    @Test
    fun `adjusts whitespace length of SynchronizedBreakToken when MarkerToken is between`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                SynchronizedBreakToken(whitespaceLength = 0),
                MarkerToken,
                WhitespaceToken(length = 0, content = " ")
            )

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(listOf(SynchronizedBreakToken(whitespaceLength = 1)))
    }

    @Test
    fun `consolidates ForcedBreakToken with ClosingSynchronizedBreakToken`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(ForcedBreakToken(count = 1), ClosingSynchronizedBreakToken(whitespaceLength = 1))

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(listOf(ClosingForcedBreakToken))
    }

    @Test
    fun `does not include the length of following tokens in the length of a WhitespaceToken`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                WhitespaceToken(content = " "),
                LeafNodeToken("any token"),
                WhitespaceToken(content = " "),
                LeafNodeToken("another token")
            )

        val result = subject.preprocess(input)

        assertThat(result).contains(WhitespaceToken(length = 10, content = " "))
    }

    @Test
    fun `adds length of string wrapping tokens to LiteralWhitespaceToken when in string literal`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                BeginToken(State.STRING_LITERAL),
                LiteralWhitespaceToken(content = " "),
                LeafNodeToken("a token"),
                LiteralWhitespaceToken(content = " "),
                LeafNodeToken("\""),
                EndToken
            )

        val result = subject.preprocess(input)

        assertThat(result).contains(LiteralWhitespaceToken(length = 11, content = " "))
    }

    @Test
    fun `adds len of string wrapping tokens to non-initial LiteralWT in string literal`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                BeginToken(State.STRING_LITERAL),
                LiteralWhitespaceToken(content = " "),
                LeafNodeToken("a token"),
                LiteralWhitespaceToken(content = " "),
                LeafNodeToken("another token"),
                LiteralWhitespaceToken(content = " "),
                LeafNodeToken("\""),
                EndToken
            )

        val result = subject.preprocess(input)

        assertThat(result).contains(LiteralWhitespaceToken(length = 17, content = " "))
    }

    @Test
    fun `adds len of str termination token to LiteralWhitespaceToken at end of string literal`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                BeginToken(State.STRING_LITERAL),
                LiteralWhitespaceToken(content = " "),
                LeafNodeToken("a token"),
                LeafNodeToken("\""),
                EndToken
            )

        val result = subject.preprocess(input)

        assertThat(result).contains(LiteralWhitespaceToken(length = 9, content = " "))
    }

    @Test
    fun `includes following leaf tokens in length of whitespace token at end of string literal`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                BeginToken(State.STRING_LITERAL),
                LiteralWhitespaceToken(content = " "),
                LeafNodeToken("\""),
                EndToken,
                LeafNodeToken(" "),
                LeafNodeToken("+")
            )

        val result = subject.preprocess(input)

        assertThat(result).contains(LiteralWhitespaceToken(length = 4, content = " "))
    }

    @Test
    fun `counts empty trailing LeafNodeToken as end of string`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                BeginToken(State.STRING_LITERAL),
                LiteralWhitespaceToken(content = " "),
                LeafNodeToken("a token"),
                LeafNodeToken("\""),
                EndToken
            )

        val result = subject.preprocess(input)

        assertThat(result).contains(LiteralWhitespaceToken(length = 9, content = " "))
    }

    @ParameterizedTest
    @MethodSource("tokenLengthCases")
    fun `outputs a BeginToken, EndToken pair with length`(token: Token, lengthExpected: Int) {
        val subject = TokenPreprocessor()
        val input = listOf(BeginToken(state = State.CODE), token, EndToken)

        val result = subject.preprocess(input)

        assertThat(result).contains(BeginToken(length = lengthExpected, state = State.CODE))
    }

    @Test
    fun `outputs a BeginToken, EndToken pair with length of two tokens`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                BeginToken(state = State.CODE),
                LeafNodeToken("token 1"),
                LeafNodeToken("token 2"),
                EndToken
            )

        val result = subject.preprocess(input)

        assertThat(result)
            .isEqualTo(
                listOf(
                    BeginToken(length = 14, state = State.CODE),
                    LeafNodeToken("token 1"),
                    LeafNodeToken("token 2"),
                    EndToken
                )
            )
    }

    @ParameterizedTest
    @MethodSource("tokenLengthCases")
    fun `outputs a BeginWeakToken with the correct length`(token: Token, lengthExpected: Int) {
        val subject = TokenPreprocessor()
        val input = listOf(BeginWeakToken(), token, EndToken)

        val result = subject.preprocess(input)

        assertThat(result).contains(BeginWeakToken(length = lengthExpected))
    }

    @Test
    fun `moves an EndToken to after a following LeafNodeToken`() {
        val subject = TokenPreprocessor()
        val input = listOf(BeginToken(state = State.CODE), EndToken, LeafNodeToken("token"))

        val result = subject.preprocess(input)

        assertThat(result)
            .isEqualTo(
                listOf(BeginToken(length = 5, state = State.CODE), LeafNodeToken("token"), EndToken)
            )
    }

    @Test
    fun `moves an inner EndToken to after a following LeafNodeToken`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                BeginToken(state = State.CODE),
                BeginToken(state = State.CODE),
                EndToken,
                EndToken,
                LeafNodeToken("token")
            )

        val result = subject.preprocess(input)

        assertThat(result)
            .isEqualTo(
                listOf(
                    BeginToken(length = 5, state = State.CODE),
                    BeginToken(length = 5, state = State.CODE),
                    LeafNodeToken("token"),
                    EndToken,
                    EndToken
                )
            )
    }

    @Test
    fun `outputs BeginToken using the state of the input token`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(BeginToken(state = State.STRING_LITERAL), LeafNodeToken("any token"), EndToken)

        val result = subject.preprocess(input)

        assertThat(result).contains(BeginToken(length = 9, state = State.STRING_LITERAL))
    }

    @Test
    fun `outputs BeginToken, EndToken pair from start of block for BlockFromMarkerToken`() {
        val subject = TokenPreprocessor()
        val input = listOf(LeafNodeToken("any token"), BlockFromMarkerToken)

        val result = subject.preprocess(input)

        assertThat(result)
            .isEqualTo(
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
        val input = listOf(LeafNodeToken("any token"), MarkerToken, BlockFromMarkerToken)

        val result = subject.preprocess(input)

        assertThat(result)
            .isEqualTo(
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
        val input = listOf(LeafNodeToken("any token"), MarkerToken)

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(listOf(LeafNodeToken("any token")))
    }

    @Test
    fun `maintains tokens following dropped MarkerToken`() {
        val subject = TokenPreprocessor()
        val input = listOf(MarkerToken, LeafNodeToken("any token"))

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(listOf(LeafNodeToken("any token")))
    }

    @Test
    fun `does not output BeginToken from MarkerToken in subblock`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                LeafNodeToken("any token"),
                BeginToken(state = State.CODE),
                MarkerToken,
                EndToken,
                BlockFromMarkerToken
            )

        val result = subject.preprocess(input)

        assertThat(result)
            .isEqualTo(
                listOf(
                    BeginToken(length = 9, state = State.CODE),
                    LeafNodeToken("any token"),
                    BeginToken(state = State.CODE),
                    EndToken,
                    EndToken
                )
            )
    }

    @Test
    fun `moves an EndToken coming from a BlockFromMarkerToken to after a LeafNodeToken`() {
        val subject = TokenPreprocessor()
        val input = listOf(MarkerToken, BlockFromMarkerToken, LeafNodeToken("token"))

        val result = subject.preprocess(input)

        assertThat(result)
            .isEqualTo(
                listOf(BeginToken(length = 5, state = State.CODE), LeafNodeToken("token"), EndToken)
            )
    }

    @Test
    fun `moves an inner EndToken followed by a BlockFromMarkerToken to after a LeafNodeToken`() {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                MarkerToken,
                BeginToken(state = State.CODE),
                EndToken,
                BlockFromMarkerToken,
                LeafNodeToken("token")
            )

        val result = subject.preprocess(input)

        assertThat(result)
            .isEqualTo(
                listOf(
                    BeginToken(length = 5, state = State.CODE),
                    BeginToken(length = 5, state = State.CODE),
                    LeafNodeToken("token"),
                    EndToken,
                    EndToken
                )
            )
    }

    @ParameterizedTest
    @MethodSource("synchronizedBreakTokenCases")
    fun `converts synchronized break into forced break when KDoc token with newline the same block`(
        synchronizedBreakToken: Token,
        unused: Token,
        expectedBreakToken: Token
    ) {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                BeginToken(state = State.CODE),
                synchronizedBreakToken,
                KDocContentToken("\n"),
                EndToken
            )

        val result = subject.preprocess(input)

        assertThat(result)
            .isEqualTo(
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
        synchronizedBreakToken: Token,
        unused1: Token,
        unused2: Token
    ) {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                BeginToken(state = State.CODE),
                BeginToken(state = State.CODE),
                synchronizedBreakToken,
                EndToken,
                KDocContentToken("\n"),
                EndToken
            )

        val result = subject.preprocess(input)

        assertThat(result)
            .isEqualTo(
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

    @ParameterizedTest
    @MethodSource("synchronizedBreakTokenCases")
    fun `does not convert synchronized break tokens in a weak subblock`(
        synchronizedBreakToken: Token,
        unused1: Token,
        unused2: Token
    ) {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                BeginToken(state = State.CODE),
                BeginWeakToken(),
                synchronizedBreakToken,
                EndToken,
                KDocContentToken("\n"),
                EndToken
            )

        val result = subject.preprocess(input)

        assertThat(result)
            .isEqualTo(
                listOf(
                    BeginToken(length = 1, state = State.CODE),
                    BeginWeakToken(length = 0),
                    synchronizedBreakToken,
                    EndToken,
                    KDocContentToken("\n"),
                    EndToken
                )
            )
    }

    @ParameterizedTest
    @MethodSource("synchronizedBreakTokenCases")
    fun `converts synchronized break into forced break when forced break in the same block`(
        synchronizedBreakToken: Token,
        forcedBreakToken: Token,
        expectedBreakToken: Token
    ) {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                BeginToken(state = State.CODE),
                synchronizedBreakToken,
                forcedBreakToken,
                EndToken
            )

        val result = subject.preprocess(input)

        assertThat(result)
            .isEqualTo(
                listOf(
                    BeginToken(length = 0, state = State.CODE),
                    expectedBreakToken,
                    forcedBreakToken,
                    EndToken
                )
            )
    }

    @ParameterizedTest
    @MethodSource("synchronizedBreakTokenCases")
    fun `converts synchronized break into forced break when force sync breaks in the same block`(
        synchronizedBreakToken: Token,
        unused: Token,
        expectedBreakToken: Token
    ) {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                BeginToken(state = State.CODE),
                synchronizedBreakToken,
                ForceSynchronizedBreaksInBlockToken,
                EndToken
            )

        val result = subject.preprocess(input)

        assertThat(result)
            .isEqualTo(
                listOf(BeginToken(length = 0, state = State.CODE), expectedBreakToken, EndToken)
            )
    }

    @ParameterizedTest
    @MethodSource("synchronizedBreakTokenCases")
    fun `converts sync break into forced break when force sync breaks in the same marker block`(
        synchronizedBreakToken: Token,
        unused: Token,
        expectedBreakToken: Token
    ) {
        val subject = TokenPreprocessor()
        val input =
            listOf(
                MarkerToken,
                synchronizedBreakToken,
                ForceSynchronizedBreaksInBlockToken,
                BlockFromMarkerToken
            )

        val result = subject.preprocess(input)

        assertThat(result)
            .isEqualTo(
                listOf(BeginToken(length = 0, state = State.CODE), expectedBreakToken, EndToken)
            )
    }

    @ParameterizedTest
    @MethodSource("synchronizedBreakTokenCases")
    fun `removes SynchronizedBreakTokens which immediately follow ForcedBreakTokens`(
        synchronizedBreakToken: Token,
        unused: Token,
        forcedBreakToken: Token
    ) {
        val subject = TokenPreprocessor()
        val input = listOf(forcedBreakToken, synchronizedBreakToken)

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(listOf(forcedBreakToken))
    }

    @ParameterizedTest
    @MethodSource("whitespaceCommentConversionCases")
    fun `converts whitespace with newlines preceding comment block into forced break`(
        whitespaceToken: Token,
        commentState: State,
        expectedToken: Token
    ) {
        val subject = TokenPreprocessor()
        val input = listOf(whitespaceToken, BeginToken(commentState), EndToken)

        val result = subject.preprocess(input)

        assertThat(result).isEqualTo(listOf(expectedToken, BeginToken(commentState), EndToken))
    }

    @Test
    fun `strips ForceSynchronizedBreaksInBlockToken from output`() {
        val subject = TokenPreprocessor()
        val input = listOf(ForceSynchronizedBreaksInBlockToken)

        val result = subject.preprocess(input)

        assertThat(result).isEmpty()
    }

    companion object {
        @JvmStatic
        fun tokenLengthCases(): List<Arguments> =
            listOf(
                Arguments.of(LeafNodeToken("any token"), 9),
                Arguments.of(KDocContentToken("any content"), 11),
                Arguments.of(WhitespaceToken(content = "  "), 1),
                Arguments.of(WhitespaceToken(content = ""), 0),
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
                Arguments.of(
                    SynchronizedBreakToken(whitespaceLength = 0),
                    ForcedBreakToken(count = 1),
                    ForcedBreakToken(count = 1)
                ),
                Arguments.of(
                    ClosingSynchronizedBreakToken(whitespaceLength = 0),
                    ForcedBreakToken(count = 1),
                    ClosingForcedBreakToken
                ),
                Arguments.of(
                    SynchronizedBreakToken(whitespaceLength = 0),
                    ClosingForcedBreakToken,
                    ForcedBreakToken(count = 1)
                ),
                Arguments.of(
                    ClosingSynchronizedBreakToken(whitespaceLength = 0),
                    ClosingForcedBreakToken,
                    ClosingForcedBreakToken
                )
            )

        @JvmStatic
        fun whitespaceCommentConversionCases(): List<Arguments> =
            listOf(
                Arguments.of(
                    WhitespaceToken(" "),
                    State.LINE_COMMENT,
                    WhitespaceToken(" ", length = 1)
                ),
                Arguments.of(
                    WhitespaceToken("\n"),
                    State.LINE_COMMENT,
                    ForcedBreakToken(count = 1)
                ),
                Arguments.of(
                    WhitespaceToken("\n\n"),
                    State.LINE_COMMENT,
                    ForcedBreakToken(count = 2)
                ),
                Arguments.of(
                    WhitespaceToken("\n\n\n"),
                    State.LINE_COMMENT,
                    ForcedBreakToken(count = 2)
                ),
                Arguments.of(
                    WhitespaceToken("\n"),
                    State.TODO_COMMENT,
                    ForcedBreakToken(count = 1)
                ),
                Arguments.of(WhitespaceToken("\n"), State.LONG_COMMENT, ForcedBreakToken(count = 1))
            )
    }
}
