package org.kotlin.formatter.output

import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.ClosingForcedBreakToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class PrinterTest {
    @Test
    fun `outputs a leaf node directly`() {
        val subject = subject()

        val result = subject.print(listOf(LeafNodeToken("variable")))

        assertThat(result).isEqualTo("variable")
    }

    @Test
    fun `outputs whitespace as a single space when the node after it fits on the line`() {
        val subject = subject(maxLineLength = 80)

        val result =
            subject.print(
                listOf(
                    WhitespaceToken(length = 80, content = " "),
                    LeafNodeToken("After whitespace")
                )
            )

        assertThat(result).isEqualTo(" After whitespace")
    }

    @ParameterizedTest
    @MethodSource("breakTokenCases")
    fun `does not output whitespace immediately before a line break`(token: Token) {
        val subject = subject()

        val result = subject.print(listOf(WhitespaceToken(length = 1, content = "   "), token))

        assertThat(result).doesNotContain("   ")
    }

    @Test
    fun `outputs indentation if whitespace does not fit on the line`() {
        val subject = subject(maxLineLength = 80, continuationIndent = 4)

        val result =
            subject.print(
                listOf(
                    WhitespaceToken(length = 81, content = " "),
                    LeafNodeToken("After whitespace")
                )
            )

        assertThat(result).isEqualTo("\n    After whitespace")
    }

    @Test
    fun `outputs additional indentation inside a block`() {
        val subject = subject(maxLineLength = 80, continuationIndent = 4)

        val result = subject.print(
            listOf(
                WhitespaceToken(length = 81, content = " "),
                BeginToken(length = 0, state = State.CODE),
                LeafNodeToken("variable"),
                WhitespaceToken(length = 81, content = " "),
                LeafNodeToken("variable")
            )
        )

        assertThat(result).isEqualTo("\n    variable\n        variable")
    }

    @Test
    fun `returns to original indentation after block ends`() {
        val subject = subject(maxLineLength = 80, continuationIndent = 4)

        val result = subject.print(
            listOf(
                WhitespaceToken(length = 81, content = " "),
                BeginToken(length = 0, state = State.CODE),
                LeafNodeToken("variable"),
                EndToken,
                WhitespaceToken(length = 81, content = " "),
                LeafNodeToken("variable")
            )
        )

        assertThat(result).isEqualTo("\n    variable\n    variable")
    }

    @Test
    fun `inserts a break if remaining space is too low`() {
        val subject = subject(maxLineLength = 80, continuationIndent = 4)

        val result = subject.print(
            listOf(
                LeafNodeToken("variable"),
                WhitespaceToken(length = 73, content = " "),
                LeafNodeToken("variable")
            )
        )

        assertThat(result).isEqualTo("variable\n    variable")
    }

    @Test
    fun `considers printed whitespace also when determining remaining space on the line`() {
        val subject = subject(maxLineLength = 80, continuationIndent = 4)

        val result = subject.print(
            listOf(
                WhitespaceToken(length = 1, content = " "),
                LeafNodeToken("variable"),
                WhitespaceToken(length = 72, content = " "),
                LeafNodeToken("variable")
            )
        )

        assertThat(result).isEqualTo(" variable\n    variable")
    }

    @Test
    fun `considers existing indentation also when determining remaining space on the line`() {
        val subject = subject(maxLineLength = 80, continuationIndent = 4)

        val result = subject.print(
            listOf(
                WhitespaceToken(length = 81, content = " "),
                LeafNodeToken("variable"),
                WhitespaceToken(length = 69, content = " "),
                LeafNodeToken("variable")
            )
        )

        assertThat(result).isEqualTo("\n    variable\n    variable")
    }

    @Test
    fun `uses current indentation and not position of beginning of block for cont indent`() {
        val subject = subject(maxLineLength = 80, continuationIndent = 4)

        val result = subject.print(
            listOf(
                LeafNodeToken("outside block"),
                BeginToken(length = 0, state = State.CODE),
                WhitespaceToken(length = 81, content = " "),
                LeafNodeToken("in block")
            )
        )

        assertThat(result).isEqualTo("outside block\n    in block")
    }

    @Test
    fun `indents with standard indent on forced break`() {
        val subject = subject(standardIndent = 2)

        val result = subject.print(
            listOf(
                ForcedBreakToken(count = 1)
            )
        )

        assertThat(result).isEqualTo("\n  ")
    }

    @Test
    fun `does not indent first line on forced break with count 2`() {
        val subject = subject(standardIndent = 2)

        val result = subject.print(
            listOf(
                ForcedBreakToken(count = 2)
            )
        )

        assertThat(result).isEqualTo("\n\n  ")
    }

    @Test
    fun `considers current indent when applying forced break`() {
        val subject = subject(standardIndent = 2)

        val result = subject.print(
            listOf(
                ForcedBreakToken(count = 1),
                BeginToken(length = 0, state = State.CODE),
                ForcedBreakToken(count = 1),
                LeafNodeToken("variable")
            )
        )

        assertThat(result).isEqualTo("\n  \n    variable")
    }

    @Test
    fun `inserts a break with no indent on ClosingForcedBreakToken`() {
        val subject = subject()

        val result = subject.print(
            listOf(
                ClosingForcedBreakToken
            )
        )

        assertThat(result).isEqualTo("\n")
    }
    
    @Test
    fun `breaks at synchronized break if parent block does not fit on line`() {
        val subject = subject(maxLineLength = 80, continuationIndent = 4)

        val result = subject.print(
            listOf(
                BeginToken(length = 81, state = State.CODE),
                SynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken("variable")
            )
        )

        assertThat(result).isEqualTo("\n    variable")
    }

    @Test
    fun `considers current position in line when deciding to break at synchronized break`() {
        val subject = subject(maxLineLength = 80, continuationIndent = 4)

        val result = subject.print(
            listOf(
                LeafNodeToken("outside block"),
                BeginToken(length = 68, state = State.CODE),
                SynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken("inside block")
            )
        )

        assertThat(result).isEqualTo("outside block\n    inside block")
    }

    @Test
    fun `outputs simple whitespace at synchronized break if parent block fits on line`() {
        val subject = subject(maxLineLength = 80, continuationIndent = 4)

        val result = subject.print(
            listOf(
                BeginToken(length = 80, state = State.CODE),
                SynchronizedBreakToken(whitespaceLength = 2),
                LeafNodeToken("variable")
            )
        )

        assertThat(result).isEqualTo("  variable")
    }

    @Test
    fun `considers current indent when determining whether to break at synchronized break`() {
        val subject = subject(maxLineLength = 80, continuationIndent = 4)

        val result = subject.print(
            listOf(
                LeafNodeToken("outside block"),
                BeginToken(length = 0, state = State.CODE),
                WhitespaceToken(length = 81, content = ""),
                BeginToken(length = 77, state = State.CODE),
                SynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken("inside block")
            )
        )

        assertThat(result).isEqualTo("outside block\n    \n        inside block")
    }
    
    @Test
    fun `breaks at closing synchronized break if parent does not fit on line`() {
        val subject = subject(maxLineLength = 80)

        val result = subject.print(
            listOf(
                BeginToken(length = 81, state = State.CODE),
                ClosingSynchronizedBreakToken,
                LeafNodeToken("variable")
            )
        )

        assertThat(result).isEqualTo("\nvariable")
    }

    @Test
    fun `does not break at closing synchronized break if parent fits on line`() {
        val subject = subject(maxLineLength = 80)

        val result = subject.print(
            listOf(
                BeginToken(length = 80, state = State.CODE),
                ClosingSynchronizedBreakToken,
                LeafNodeToken("variable")
            )
        )

        assertThat(result).isEqualTo("variable")
    }

    @ParameterizedTest
    @MethodSource("commentCases")
    fun `breaks comment by adding comment marker`(
        commentState: State,
        commentPrefix: String
    ) {
        val subject = subject(maxLineLength = 80)

        val result = subject.print(
            listOf(
                BeginToken(length = 0, state = commentState),
                WhitespaceToken(length = 81, content = " "),
                LeafNodeToken("Comment")
            )
        )

        assertThat(result).isEqualTo("\n${commentPrefix}Comment")
    }

    @Test
    fun `does not insert comment marker on ClosingSynchronizedBreakToken in long comment`() {
        val subject = subject(maxLineLength = 80)

        val result = subject.print(
            listOf(
                BeginToken(length = 81, state = State.LONG_COMMENT),
                ClosingSynchronizedBreakToken,
                LeafNodeToken("Comment")
            )
        )

        assertThat(result).isEqualTo("\n Comment")
    }

    @ParameterizedTest
    @MethodSource("commentCases")
    fun `inserts initial whitespace when breaking line comment`(
        commentState: State,
        commentPrefix: String
    ) {
        val subject = subject(maxLineLength = 80)

        val result = subject.print(
            listOf(
                LeafNodeToken("outside block"),
                BeginToken(length = 0, state = State.CODE),
                WhitespaceToken(length = 81, content = ""),
                BeginToken(length = 0, state = commentState),
                WhitespaceToken(length = 81, content = " "),
                LeafNodeToken("Comment")
            )
        )

        assertThat(result).isEqualTo("outside block\n        \n        ${commentPrefix}Comment")
    }

    @ParameterizedTest
    @MethodSource("commentCases")
    fun `sets remaining space according to current indent when breaking line comment`(
        commentState: State,
        commentPrefix: String
    ) {
        val subject = subject(maxLineLength = 80)

        val result = subject.print(
            listOf(
                LeafNodeToken("outside block"),
                BeginToken(length = 0, state = State.CODE),
                WhitespaceToken(length = 81, content = ""),
                BeginToken(length = 0, state = commentState),
                WhitespaceToken(length = 81, content = " "),
                LeafNodeToken("Comment"),
                WhitespaceToken(length = 65, content = " "),
                LeafNodeToken("Comment")
            )
        )

        assertThat(result)
            .isEqualTo(
                "outside block\n        \n        ${commentPrefix}Comment\n        " +
                "${commentPrefix}Comment"
            )
    }

    @Test
    fun `breaks a string literal with concatenation operator and preserving original whitespace`() {
        val subject = subject(maxLineLength = 80, continuationIndent = 4)

        val result = subject.print(
            listOf(
                BeginToken(length = 0, state = State.STRING_LITERAL),
                LeafNodeToken("Content"),
                WhitespaceToken(length = 74, content = "  "),
                LeafNodeToken("Content")
            )
        )

        assertThat(result).isEqualTo("Content  \" +\n    \"Content")
    }

    @Test
    fun `preserves whitespace when not breaking lines when in string literal`() {
        val subject = subject()

        val result = subject.print(
            listOf(
                BeginToken(length = 0, state = State.STRING_LITERAL),
                WhitespaceToken(length = 0, content = "  "),
                LeafNodeToken("After whitespace")
            )
        )

        assertThat(result).isEqualTo("  After whitespace")
    }

    @Test
    fun `puts whitespace on next line if it does not fit in string literal`() {
        val subject = subject(maxLineLength = 10, continuationIndent = 4)

        val result = subject.print(
            listOf(
                BeginToken(length = 0, state = State.STRING_LITERAL),
                LeafNodeToken("Content"),
                WhitespaceToken(length = 10, content = " "),
                LeafNodeToken("Content")
            )
        )

        assertThat(result).isEqualTo("Content\" +\n    \" Content")
    }

    @Test
    fun `breaks with enough room for string closing characters in string literal`() {
        val subject = subject(maxLineLength = 9, continuationIndent = 4)

        val result = subject.print(
            listOf(
                BeginToken(length = 0, state = State.STRING_LITERAL),
                LeafNodeToken("Content"),
                WhitespaceToken(length = 2, content = ""),
                LeafNodeToken("Content")
            )
        )

        assertThat(result).isEqualTo("Content\" +\n    \"Content")
    }

    @Test
    fun `respects current indent when breaking string literal`() {
        val subject = subject(maxLineLength = 80, continuationIndent = 4)

        val result = subject.print(
            listOf(
                LeafNodeToken("outside block"),
                BeginToken(length = 0, state = State.CODE),
                WhitespaceToken(length = 81, content = ""),
                BeginToken(length = 0, state = State.STRING_LITERAL),
                LeafNodeToken("Content"),
                WhitespaceToken(length = 80, content = ""),
                LeafNodeToken("Content")
            )
        )

        assertThat(result).isEqualTo("outside block\n    Content\" +\n        \"Content")
    }

    @Test
    fun `does not break multiline string literal`() {
        val subject = subject(maxLineLength = 80)

        val result = subject.print(
            listOf(
                BeginToken(length = 0, state = State.MULTILINE_STRING_LITERAL),
                WhitespaceToken(length = 81, content = "  "),
                LeafNodeToken("After whitespace")
            )
        )

        assertThat(result).isEqualTo("  After whitespace")
    }

    private fun subject(
        maxLineLength: Int = 100,
        standardIndent: Int = 2,
        continuationIndent: Int = 8
    ) = Printer(
        maxLineLength = maxLineLength,
        standardIndent = standardIndent,
        continuationIndent = continuationIndent
    )

    companion object {
        @JvmStatic
        fun commentCases(): List<Arguments> =
            listOf(
                Arguments.of(State.LINE_COMMENT, "// "),
                Arguments.of(State.TODO_COMMENT, "//  "),
                Arguments.of(State.LONG_COMMENT, " * "),
                Arguments.of(State.KDOC_DIRECTIVE, " *     ")
            )

        @JvmStatic
        fun breakTokenCases(): List<Arguments> =
            listOf(
                Arguments.of(ForcedBreakToken(count = 1)),
                Arguments.of(SynchronizedBreakToken(whitespaceLength = 1)),
                Arguments.of(ClosingForcedBreakToken),
                Arguments.of(ClosingSynchronizedBreakToken)
            )
    }
}
