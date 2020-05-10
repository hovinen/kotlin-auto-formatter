package org.kotlin.formatter

/**
 * The state in which a [org.kotlin.formatter.output.Printer] operates while processing tokens in
 * a block marked by a [BeginToken], [EndToken] pair.
 *
 * This state influences how line breaks are determined and how they are inserted.
 */
enum class State {
    /**
     * Ordinary Kotlin code.
     *
     * Line breaks are determined by the usual rules and are performed by indentation only, as
     * described in [org.kotlin.formatter.output.Printer].
     *
     * Existing non-essential line breaks are removed in this state. For example:
     *
     * ```
     * val aValue = aVariable +
     *    anotherVariable
     * ```
     *
     * becomes
     *
     * ```
     * val aValue = aVariable + anotherVariable
     * ```
     *
     * provided the column limit allows it.
     */
    CODE,

    /**
     * A single line string literal.
     *
     * Line breaks are determined by the usual rules. They are inserted by closing the string,
     * adding a concatenation operator, indenting with the continuation indent, and beginning a new
     * string. For example:
     *
     * ```
     * "A long string which should wrap"
     * ```
     *
     * becomes
     *
     * ```
     * "A long string which " +
     *     "should wrap"
     * ```
     *
     * Existing line breaks cannot exist due to Kotlin syntax.
     */
    STRING_LITERAL,

    /**
     * A multiline string literal.
     *
     * Line breaks are not inserted here and existing line breaks are respected. The column limit is
     * ignored.
     */
    MULTILINE_STRING_LITERAL,

    /**
     * A single line comment, i.e., one beginning with `//`.
     *
     * Line breaks are inserted at word boundaries to respect the column limit. A single line
     * comment is inserted on the next line, which is indented to the same point as the previous
     * comment.
     *
     * Existing line breaks are respected.
     */
    LINE_COMMENT,

    /**
     * A single line comment which introduces a TODO.
     *
     * Line breaks are inserted at word boundaries to respect the column limit. A single line
     * comment is inserted on the next line, which is indented to the same point as the previous
     * comment. Following text is inserted by an additional space.
     *
     * For example, the following:
     *
     * ```
     * // TODO(...): Do some stuff which has a long description and requires wrapping.
     * ```
     *
     * becomes:
     *
     * ```
     * // TODO(...): Do some stuff which has a long description and
     * //  requires wrapping.
     * ```
     *
     * Existing line breaks are respected.
     */
    TODO_COMMENT,

    /**
     * A multi-line comment, i.e., one demarcated with `/*` and `*/`.
     *
     * This includes KDoc.
     *
     * Line breaks are inserted at word boundaries to respect the column limit. A leading asterisk
     * is inserted on the next line aligned with the previous line's asterisk.
     *
     * Existing line breaks in ordinary long comments are respected.
     *
     * Inside KDoc, line breaks within paragraphs may be removed while upholding the column limit.
     * For example:
     *
     * ```
     * /**
     *  * Some KDoc
     *  * with some stuff.
     *  */
     * ```
     *
     * becomes:
     *
     * ```
     * /**
     *  * Some KDoc with some stuff.
     *  */
     * ```
     *
     * or even:
     *
     * ```
     * /** Some KDoc with some stuff. */
     * ```
     *
     * depending on which variant fits in the column limit.
     */
    LONG_COMMENT,

    /**
     * A tag within KDoc.
     *
     * The rules are similar to those of [LONG_COMMENT] above, except that inserted line breaks
     * result in a continuation indent. For example:
     *
     * ```
     * /**
     *  * Some KDoc.
     *  *
     *  * @param parameter a long description of a parameter which should wrap
     *  */
     * ```
     *
     * becomes:
     *
     * ```
     * /**
     *  * Some KDoc.
     *  *
     *  * @param parameter a long description of a parameter
     *  *     which should wrap
     *  */
     * ```
     */
    KDOC_TAG,

    /**
     * A series of `package` and `import` statements.
     *
     * This state disables line breaking, and existing non-essential line breaks are removed. This
     * is as per the rules of the
     * [Google Kotlin style guide](https://developer.android.com/kotlin/style-guide), by which
     * `package` and `import` statments are explicitly excluded from the column limit.
     */
    PACKAGE_IMPORT
}
