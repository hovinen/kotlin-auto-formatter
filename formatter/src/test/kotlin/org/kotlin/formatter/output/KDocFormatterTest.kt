package org.kotlin.formatter.output

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class KDocFormatterTest {
    @Test
    fun `leaves empty input as is`() {
        val subject = KDocFormatter(maxLineLength = 10)

        val result = subject.format("")

        assertThat(result).isEqualTo("")
    }

    @Test
    fun `leaves short paragraphs as is`() {
        val subject = KDocFormatter(maxLineLength = 10)

        val result = subject.format("Some KDoc.")

        assertThat(result).isEqualTo("Some KDoc.")
    }

    @Test
    fun `consolidates single line breaks`() {
        val subject = KDocFormatter(maxLineLength = 10)

        val result = subject.format("Some\nKDoc.")

        assertThat(result).isEqualTo("Some KDoc.")
    }

    @Test
    fun `wraps line at column limit`() {
        val subject = KDocFormatter(maxLineLength = 5)

        val result = subject.format("Some KDoc.")

        assertThat(result).isEqualTo("Some\nKDoc.")
    }

    @Test
    fun `wraps line at column limit on second line`() {
        val subject = KDocFormatter(maxLineLength = 10)

        val result = subject.format("Some KDoc some KDoc.")

        assertThat(result).isEqualTo("Some KDoc\nsome KDoc.")
    }

    @Test
    fun `keeps paragraphs separate`() {
        val subject = KDocFormatter(maxLineLength = 10)

        val result = subject.format("Some\n\nKDoc.")

        assertThat(result).isEqualTo("Some\n\nKDoc.")
    }

    @Test
    fun `removes extraneous blank lines`() {
        val subject = KDocFormatter(maxLineLength = 10)

        val result = subject.format("Some\n\n\nKDoc.")

        assertThat(result).isEqualTo("Some\n\nKDoc.")
    }

    @Test
    fun `leaves tables untouched`() {
        val subject = KDocFormatter(maxLineLength = 10)

        val result = subject.format(
            """
                A | Table
                Another | Row
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                A | Table
                Another | Row
            """.trimIndent()
        )
    }

    @Test
    fun `preserves blank lines after tables`() {
        val subject = KDocFormatter(maxLineLength = 20)

        val result = subject.format(
            """
                A | Table
                
                A paragraph
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                A | Table
                
                A paragraph
            """.trimIndent()
        )
    }

    @Test
    fun `does not break inside a link`() {
        val subject = KDocFormatter(maxLineLength = 10)

        val result = subject.format("Some [A link](another-file.md) KDoc.")

        assertThat(result).isEqualTo("Some\n[A link](another-file.md)\nKDoc.")
    }

    @Test
    fun `does not break inside a second link`() {
        val subject = KDocFormatter(maxLineLength = 30)

        val result = subject.format("Some [A link](a.md) KDoc [A link](b.md).")

        assertThat(result).isEqualTo("Some [A link](a.md) KDoc\n[A link](b.md).")
    }

    @Test
    fun `does not append whitespace after a link`() {
        val subject = KDocFormatter(maxLineLength = 10)

        val result = subject.format("[A link](another-file.md).")

        assertThat(result).isEqualTo("[A link](another-file.md).")
    }

    @Test
    fun `does not insert whitespace before a link`() {
        val subject = KDocFormatter(maxLineLength = 50)

        val result = subject.format("something[A link](another-file.md)")

        assertThat(result).isEqualTo("something[A link](another-file.md)")
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "@param",
        "@property",
        "@return",
        "@throws",
        "@exception",
        "@see",
        "@constructor",
        "@receiver",
        "@author",
        "@since",
        "@suppress"
    ])
    fun `keeps tags in separate paragraphs`(tag: String) {
        val subject = KDocFormatter(maxLineLength = 50)

        val result =
            subject.format(
                """
                    @param aParameter A parameter.
                    $tag anotherTag
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    @param aParameter A parameter.
                    $tag anotherTag
                """.trimIndent()
            )
    }

    @Test
    fun `indents continuation lines of tag`() {
        val subject = KDocFormatter(maxLineLength = 50)

        val result =
            subject.format(
                """
                    @param aParameter A parameter with some additional text which wraps.
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    @param aParameter A parameter with some additional
                        text which wraps.
                """.trimIndent()
            )
    }

    @Test
    fun `indents further lines of a tag based on continuation indent`() {
        val subject = KDocFormatter(maxLineLength = 50)

        val result =
            subject.format(
                """
                    @param aParameter A parameter with some additional
                        text which wraps and some more text which also w
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    @param aParameter A parameter with some additional
                        text which wraps and some more text which also
                        w
                """.trimIndent()
            )
    }

    @Test
    fun `properly indents wrapped unordered lists`() {
        val subject = KDocFormatter(maxLineLength = 50)

        val result =
            subject.format(
                """
                    A paragraph.
                    
                     * A list item which should wrap because it is so long.
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    A paragraph.
                    
                     * A list item which should wrap because it is so
                       long.
                """.trimIndent()
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["*", "-", "+"])
    fun `handles all marker types for unordered lists`(symbol: String) {
        val subject = KDocFormatter(maxLineLength = 50)

        val result =
            subject.format(
                """
                    A paragraph
                    
                     $symbol A list item.
                     $symbol Another list item.
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    A paragraph
                    
                     $symbol A list item.
                     $symbol Another list item.
                """.trimIndent()
            )
    }

    @Test
    fun `corrects formatting of unordered lists`() {
        val subject = KDocFormatter(maxLineLength = 50)

        val result =
            subject.format(
                """
                    A paragraph.
                    
                    * A list item which should wrap because it is so long.
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    A paragraph.
                    
                     * A list item which should wrap because it is so
                       long.
                """.trimIndent()
            )
    }

    @Test
    fun `maintains original continuation indent in paragraphs following lists`() {
        val subject = KDocFormatter(maxLineLength = 50)

        val result =
            subject.format(
                """
                     * A list item
                    
                    A paragraph which should wrap because the line is too long.
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                     * A list item
                    
                    A paragraph which should wrap because the line is
                    too long.
                """.trimIndent()
            )
    }

    @Test
    fun `properly indents wrapped ordered lists`() {
        val subject = KDocFormatter(maxLineLength = 50)

        val result =
            subject.format(
                """
                    A paragraph.
                    
                     1. A list item which should wrap because it is so long.
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    A paragraph.
                    
                     1. A list item which should wrap because it is so
                        long.
                """.trimIndent()
            )
    }

    @Test
    fun `corrects formatting of ordered lists`() {
        val subject = KDocFormatter(maxLineLength = 50)

        val result =
            subject.format(
                """
                    A paragraph.
                    
                    1. A list item which should wrap because it is so long.
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    A paragraph.
                    
                     1. A list item which should wrap because it is so
                        long.
                """.trimIndent()
            )
    }

    @Test
    fun `properly indents wrapped ordered lists with two digit number`() {
        val subject = KDocFormatter(maxLineLength = 50)

        val result =
            subject.format(
                """
                    A paragraph.
                    
                     10. A list item which should wrap because it is so long.
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    A paragraph.
                    
                     10. A list item which should wrap because it is
                         so long.
                """.trimIndent()
            )
    }

    @Test
    fun `leaves preformatted code blocks as is`() {
        val subject = KDocFormatter(maxLineLength = 50)

        val result = subject.format(
            """
                ```
                    Some code
                    Some more code
                ```
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                ```
                    Some code
                    Some more code
                ```
            """.trimIndent()
        )
    }

    @Test
    fun `leaves whitespace in preformatted code blocks as is`() {
        val subject = KDocFormatter(maxLineLength = 50)

        val result = subject.format(
            """
                ```
                    Some code
                
                    Some more code
                ```
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                ```
                    Some code
                
                    Some more code
                ```
            """.trimIndent()
        )
    }

    @Test
    fun `handles preformatted code blocks immediately preceded by paragraphs`() {
        val subject = KDocFormatter(maxLineLength = 50)

        val result =
            subject.format(
                """
                    A paragraph which is long enough that the text should wrap.
                    ```
                        Some code
                    ```
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    A paragraph which is long enough that the text
                    should wrap.
                    ```
                        Some code
                    ```
                """.trimIndent()
            )
    }

    @Test
    fun `handles paragraph following preformatted code blocks`() {
        val subject = KDocFormatter(maxLineLength = 50)

        val result =
            subject.format(
                """
                    ```
                        Some code
                    ```
                    A paragraph which is long enough that the text should wrap.
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    ```
                        Some code
                    ```
                    A paragraph which is long enough that the text
                    should wrap.
                """.trimIndent()
            )
    }

    @Test
    fun `preserves a blank line following a raw block`() {
        val subject = KDocFormatter(maxLineLength = 50)

        val result = subject.format(
            """
                ```
                    Some code
                ```
                
                A paragraph.
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                ```
                    Some code
                ```
                
                A paragraph.
            """.trimIndent()
        )
    }

    @Test
    fun `handles preformatted code start with following text`() {
        val subject = KDocFormatter(maxLineLength = 50)

        val result =
            subject.format(
                """
                    A paragraph which is long enough that the text should wrap.
                    ```language
                        Some code
                    ```
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    A paragraph which is long enough that the text
                    should wrap.
                    ```language
                        Some code
                    ```
                """.trimIndent()
            )
    }

    @Test
    fun `formats within block quotes`() {
        val subject = KDocFormatter(maxLineLength = 12)

        val result = subject.format("> Some KDoc a a a.")

        assertThat(result).isEqualTo("> Some KDoc\n> a a a.")
    }

    @Test
    fun `consolidates within block quotes`() {
        val subject = KDocFormatter(maxLineLength = 12)

        val result = subject.format("> Some\n> KDoc")

        assertThat(result).isEqualTo("> Some KDoc")
    }

    @Test
    fun `maintains paragraphs before block quotes`() {
        val subject = KDocFormatter(maxLineLength = 20)

        val result = subject.format("A paragraph\n> A quote")

        assertThat(result).isEqualTo("A paragraph\n> A quote")
    }

    @Test
    fun `preserves whitespace after a block quote`() {
        val subject = KDocFormatter(maxLineLength = 50)

        val result = subject.format(
            """
                > A quote.
                
                A paragraph.
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                > A quote.
                
                A paragraph.
            """.trimIndent()
        )
    }

    @Test
    fun `does not consolidate lines with atx-style headers`() {
        val subject = KDocFormatter(maxLineLength = 20)

        val result = subject.format("A paragraph\n# A header\nA paragraph")

        assertThat(result).isEqualTo("A paragraph\n\n# A header\n\nA paragraph")
    }

    @Test
    fun `preserves blank lines around headers`() {
        val subject = KDocFormatter(maxLineLength = 20)

        val result = subject.format("A paragraph\n\n# A header\n\nA paragraph")

        assertThat(result).isEqualTo("A paragraph\n\n# A header\n\nA paragraph")
    }

    @ParameterizedTest
    @ValueSource(strings = ["=", "-"])
    fun `does not consolidate lines with setext-style headers`(underlineCharacter: String) {
        val subject = KDocFormatter(maxLineLength = 20)

        val result = subject.format("A header\n$underlineCharacter\nA paragraph")

        assertThat(result).isEqualTo("A header\n$underlineCharacter\nA paragraph")
    }
}
