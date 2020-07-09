package org.kotlin.formatter.output

/**
 * A simple formatter for KDoc.
 *
 * This reformats KDoc paragraphs to fix in the given column limit [maxLineLength] and consolidates
 * paragraphs by eliminating syntactically extraneous line breaks. It respects the constructions of
 * the [Daring Fireball syntax](https://daringfireball.net/projects/markdown/syntax) of markdown, as
 * referenced by the [KDoc documentation](https://kotlinlang.org/docs/reference/kotlin-doc.html).
 *
 * KDoc tags have a continuation indent of +4, as specified in the
 * [Google Kotlin style guide](https://developer.android.com/kotlin/style-guide#block_tags).
 *
 * Markdown tables, as detected by the presence of the pipe `|` in a line, are left untouched by the
 * formatter.
 */
class KDocFormatter(private val maxLineLength: Int) {
    /**
     * Formats the given string [input] as KDoc, returning the formatted string.
     *
     * The input is expected to be KDoc without the leading asterisk and whitespace characters.
     */
    fun format(input: String): String =
        splitIntoBlocks(input).joinToString("\n") { it.format() }.trimEnd()

    private fun splitIntoBlocks(input: String): List<Block> {
        val result = mutableListOf<Block>()
        val linesInBlock = mutableListOf<String>()
        var continuationIndent = 0
        var state = State.PARAGRAPH

        fun pushBlock(spacingAfter: Int) {
            if (state == State.EXITING_RAW_BLOCK) {
                result.add(
                    RawBlock(linesInBlock.joinToString("\n").plus("\n".repeat(spacingAfter)))
                )
            } else if (state == State.BLOCK_QUOTE) {
                result.add(
                    RawBlock(
                        toBlockQuote(formatBlockQuote(linesInBlock.joinToString("\n"))).plus("\n")
                    )
                )
            } else if (linesInBlock.isNotEmpty()) {
                result.add(
                    Paragraph(
                        linesInBlock.joinToString("\n"),
                        spacingAfter = spacingAfter,
                        continuationIndent = continuationIndent
                    )
                )
            }
            linesInBlock.clear()
        }

        fun handleParagraphLine(line: String) {
            val unorderedListMatch = numberedListElement.matchEntire(line)
            val orderedListMatch = orderedListElement.matchEntire(line)
            when {
                line.isBlank() -> {
                    continuationIndent = 0
                    pushBlock(1)
                }
                line.startsWith("```") -> {
                    pushBlock(0)
                    state = State.RAW_BLOCK
                    linesInBlock.add(line)
                }
                line.startsWith(BLOCK_QUOTE_PREFIX) -> {
                    pushBlock(0)
                    linesInBlock.add(line.removePrefix(BLOCK_QUOTE_PREFIX))
                    state = State.BLOCK_QUOTE
                }
                line.startsWith("#") -> {
                    pushBlock(0)
                    linesInBlock.add(line)
                    pushBlock(0)
                }
                line.startsWith("=") || line.startsWith("-") -> {
                    pushBlock(0)
                    linesInBlock.add(line)
                    pushBlock(0)
                }
                isTag(line) -> {
                    pushBlock(0)
                    continuationIndent = 4
                    linesInBlock.add(line)
                }
                unorderedListMatch != null -> {
                    pushBlock(0)
                    val value = unorderedListMatch.groups[2]?.value ?: ""
                    continuationIndent = value.length + 1
                    linesInBlock.add(
                        line.replace(unorderedListMatch.groups[1]?.value ?: "", " $value")
                    )
                }
                orderedListMatch != null -> {
                    pushBlock(0)
                    continuationIndent = 3
                    linesInBlock.add(line.replace(orderedListMatch.groups[1]?.value ?: "", " * "))
                }
                line.contains('|') -> {
                    pushBlock(0)
                    state = State.EXITING_RAW_BLOCK
                    linesInBlock.add(line)
                    pushBlock(0)
                    state = State.PARAGRAPH
                }
                else -> {
                    linesInBlock.add(line)
                }
            }
        }

        for (line in input.split("\n")) {
            when (state) {
                State.PARAGRAPH -> {
                    handleParagraphLine(line)
                }
                State.EXITING_RAW_BLOCK -> {
                    pushBlock(if (line.isBlank()) 1 else 0)
                    state = State.PARAGRAPH
                    handleParagraphLine(line)
                }
                State.RAW_BLOCK -> {
                    linesInBlock.add(line)
                    if (line == "```") {
                        state = State.EXITING_RAW_BLOCK
                    }
                }
                State.BLOCK_QUOTE -> {
                    if (line.isBlank()) {
                        pushBlock(1)
                        state = State.PARAGRAPH
                    } else {
                        linesInBlock.add(line.removePrefix(BLOCK_QUOTE_PREFIX))
                    }
                }
            }
        }
        pushBlock(1)
        return result
    }

    private enum class State {
        PARAGRAPH, RAW_BLOCK, BLOCK_QUOTE, EXITING_RAW_BLOCK
    }

    private fun formatBlockQuote(content: String) =
        KDocFormatter(maxLineLength = maxLineLength - BLOCK_QUOTE_PREFIX.length).format(content)

    private fun toBlockQuote(content: String) = content.split("\n").joinToString("\n") { "> $it" }

    private fun isTag(line: String) = TAGS.any { line.startsWith(it) }

    private interface Block {
        fun format(): String
    }

    private inner class Paragraph(
        content: String,
        private val spacingAfter: Int,
        private val continuationIndent: Int
    ) : Block {
        private val tokens = tokenize(content)

        override fun format(): String {
            val builder = StringBuilder()
            builder.append(tokens.first())
            var column = tokens.first().length
            for (token in tokens.tail()) {
                if (column + token.length + 1 > maxLineLength) {
                    builder.append("\n${" ".repeat(continuationIndent)}")
                    column = token.length + continuationIndent
                } else {
                    builder.append(" ")
                    column += token.length + 1
                }
                builder.append(token)
            }
            for (i in 0 until spacingAfter) {
                builder.append('\n')
            }
            return builder.toString()
        }

        private fun tokenize(input: String): List<String> {
            val match = linkElement.find(input)
            return if (match != null) {
                tokenizeBySpaces(input.substring(0, match.range.first).trimEnd()).plus(match.value)
                    .plus(tokenize(input.substring(match.range.last + 1).trimStart()))
            } else {
                tokenizeBySpaces(input)
            }
        }

        private fun tokenizeBySpaces(input: String): List<String> =
            if (input.isNotEmpty()) input.split(Regex("\\s+")) else listOf()

        private fun <E> List<E>.tail(): List<E> = subList(1, size)
    }

    private class RawBlock(private val content: String) : Block {
        override fun format(): String = content
    }

    companion object {
        private val TAGS =
            setOf(
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
            )
        private const val BLOCK_QUOTE_PREFIX = "> "

        private val numberedListElement = Regex("(\\s*(\\d+. )).*")
        private val orderedListElement = Regex("(\\s*\\*\\s+).*")

        private val linkElement = Regex("[^\\s]*\\[.+?](\\(.+?\\))?[^\\s]*")
    }
}
