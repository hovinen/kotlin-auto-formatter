package org.kotlin.formatter

/**
 * A directive to output text or to control line breaks.
 *
 * This is the communications language between the
 * [scanner][org.kotlin.formatter.scanning.KotlinScanner] and
 * [printer][org.kotlin.formatter.output.Printer] components of the formatter. The scanner produces
 * a sequence of [Token] which are then
 * [preprocessed][org.kotlin.formatter.output.TokenPreprocessor] and output.
 */
sealed class Token

/** A directive to output the given [text]. */
data class LeafNodeToken(internal val text: String) : Token() {
    /** The length of the [text] in this token in characters. */
    internal val textLength: Int = text.length

    override fun toString(): String = "LeafNodeToken(text='$text')"
}

/** A directive to output a single space character without inserting a line break. */
fun nonBreakingSpaceToken(content: String = " "): Token = LeafNodeToken(text = content)

/**
 * A directive to output the given [content] as the content of a long comment such as KDoc.
 *
 * All line breaks in the content are indented and prefixed with the long comment marker ` * `. If
 * the token is immediately preceded by a [ForcedBreakToken], a long comment marker is also output
 * before the content so that the first line is consistent with the remaining content.
 *
 * The value of [content] is expected to be stripped of leading asterisks as well as the begin and
 * end markers. In other words, it should be suitable for passing through a separate markdown auto-
 * formatter.
 *
 * The content is otherwise output unchanged with no additional line breaking.
 */
// TODO: Support passing the content through a separate formatter from the Printer.
data class KDocContentToken(internal val content: String) : Token() {
    internal val textLength: Int = content.length

    override fun toString(): String = "KDocContentToken(content='$content')"
}

/**
 * A directive to output whitespace, possibly with a line break.
 *
 * Line breaks inserted to keep text within the column limit occur exclusively at [WhitespaceToken].
 * When encountering a [WhitespaceToken], the [printer][org.kotlin.formatter.output.Printer]
 * determines whether it plus the block or sequence of [LeafNodeToken] immediately following it fit
 * on the current line.
 *
 * If they do fit, then the token is output as a single space character.
 *
 * If they do not fit, then the whitespace content is replaced by a newline followed by a
 * continuation indent from indentation level of the containing block.
 *
 * @property content the original literal whitespace content behind the token
 * @property length the printing length of the token if no line break is made. This is one plus the
 *     length of the following block or [LeafNodeToken], since the content is replaced by a single
 *     space if no line break is performed.
 */
data class WhitespaceToken(internal val content: String, internal val length: Int = 0) : Token()

/**
 * A directive to output whitespace exactly as given, inserting a line break if needed.
 *
 * The behaviour of this token is similar to that of [WhitespaceToken], except that the original
 * whitespace content is always output. It is not shortened to a single space, nor is it replaced by
 * a line break.
 *
 * @property content the original literal whitespace content behind the token
 * @property length the printing length of the token if no line break is made. This is the length of
 *     [content] plus the length of the following [LeafNodeToken].
 */
data class LiteralWhitespaceToken(internal val content: String, internal val length: Int = 0) :
    Token()

/**
 * Returns a [Token] which inserts a point where a line break may be introduced but produces no
 * output if no line break occurs.
 */
fun emptyBreakPoint() = WhitespaceToken("")

/**
 * A directive to add one or more line breaks, indenting the following line by the standard indent
 * from the indentation of the containing block.
 *
 * If more than one line break is inserted, the intermediate (empty) lines receive no indent. If the
 * current [State] is a comment, the comment begin marker is inserted for each intermediate line.
 *
 * This token is not supported in single line string literals.
 *
 * @property count the number of line breaks to be inserted; must be at least one
 */
data class ForcedBreakToken(internal val count: Int) : Token()

/**
 * A directive to add one line break, indenting the next line to the level of the containing block.
 *
 * Compared to [ForcedBreakToken], this indents the following line by one less level of standard
 * indent.
 *
 * This token is not supported in single line string literals.
 */
object ClosingForcedBreakToken : Token()

/**
 * A directive to add a line break only if the containing block does not fit on one line.
 *
 * Similarly to [WhitespaceToken], the following line is indented with a continuation indent from
 * the containing block's indentation level. The token should always be followed by [LeafNodeToken]
 * to ensure that there is no trailing whitespace on the line in case of a break.
 *
 * @property whitespaceLength the number of spaces of whitespace to be output by this token if no
 *     line break is introduced; normally either zero or one
 */
data class SynchronizedBreakToken(
    internal val whitespaceLength: Int,
    internal val content: String = " ".repeat(whitespaceLength)
) : Token()

/**
 * A directive to add a closing line break only if the containing block does not fit on one line.
 *
 * The following line is indented at the same level as the containing block. The token should always
 * be followed by [LeafNodeToken] to ensure that there is no trailing whitespace on the line in case
 * of a break.
 *
 * @property whitespaceLength the number of spaces of whitespace to be output by this token if no
 *     line break is introduced; normally either zero or one
 */
data class ClosingSynchronizedBreakToken(
    internal val whitespaceLength: Int,
    internal val content: String = " ".repeat(whitespaceLength)
) : Token()

/**
 * A directive to add line break without indent only if the containing block does not fit on one
 * line.
 *
 * The following line is not indented at all. This is intended for cases that the following line
 * would be completely empty and therefore any indentation would cause trailing whitespace.
 *
 * @property whitespaceLength the number of spaces of whitespace to be output by this token if no
 *     line break is introduced; normally either zero or one
 */
data class NonIndentingSynchronizedBreakToken(internal val whitespaceLength: Int) : Token()

/**
 * A directive to begin a new block in the given [State].
 *
 * The [printer][org.kotlin.formatter.output.Printer] prefers to insert line breaks outside of
 * blocks when possible. Thus grouping a sequence of [Token] into a block may cause a line break to
 * appear before the block rather than within it.
 *
 * Blocks are delimited in a list of [Token] by [BeginToken] and [EndToken] like parentheses.
 *
 * The parameter [length] is the display length of the block in tokens.
 */
data class BeginToken(internal val state: State, internal val length: Int = 0) : Token()

/**
 * A directive to begin a new weak block.
 *
 * This is similar to [BeginToken] in that it marks the beginning of a new block. Unlike
 * [BeginToken], the block it begins plays no role in calculating [WhitespaceToken.length]. This
 * implies that the formatter does not prefer to insert a line break before the block but rather may
 * insert line breaks within the block. In this sense, the block is "weak".
 *
 * A weak block is terminated by [EndToken], just as a regular block.
 */
data class BeginWeakToken(internal val length: Int = 0) : Token() {
    val beginToken = BeginToken(State.CODE, length)
}

/** A directive to end the current block. */
object EndToken : Token()

/**
 * Returns a sequence of [Token] which wraps [innerTokens] inside a [BeginToken], [EndToken] pair
 * with the given [State].
 */
fun inBeginEndBlock(innerTokens: List<Token>, state: State): List<Token> =
    listOf(BeginToken(state)).plus(innerTokens).plus(EndToken)

/**
 * A directive marking the point at which a block terminated by a [BlockFromMarkerToken] should
 * begin.
 *
 * This token is stripped by the token preprocessor before the list of tokens is passed to the
 * printer.
 */
object MarkerToken : Token()

/**
 * A directive to insert a new block starting just after the last [MarkerToken] in the current
 * block, or at the beginning of the current block if there is no such token in the current block,
 * and ending at the current position.
 *
 * This token is stripped by the token preprocessor before the list of tokens is passed to the
 * printer.
 */
object BlockFromMarkerToken : Token()

/**
 * A directive that, from this token on, newlines in the original source code should be preserved
 * when they would ordinarily be removed as redundant by the autoformatter.
 *
 * If this token appears while the formatter is already in the preserve newlines mode, it has no
 * effect.
 */
object BeginPreserveNewlinesToken : Token()

/**
 * A directive that, from this point on, newlines in the original code should be treated normally
 * and removed when they are redundant.
 *
 * If this token appears while the formatter is in regular mode, it has no effect.
 */
object EndPreserveNewlinesToken : Token()
