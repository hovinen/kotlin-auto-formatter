package org.kotlin.formatter

import org.kotlin.formatter.loading.KotlinFileLoader
import org.kotlin.formatter.output.Printer
import org.kotlin.formatter.scanning.KotlinScanner
import java.nio.file.Files
import java.nio.file.Path

/**
 * Automatic formatter for Kotlin code to fit in a given column limit.
 *
 * @property maxLineLength the maximum column limit in which to try to fit the code
 * @property continuationIndentSize the additional amount to indent when introducing a line break
 *     within an existing statement
 */
class KotlinFormatter(
    maxLineLength: Int = 100,
    standardIndentSize: Int = 4,
    continuationIndentSize: Int = 4
) {
    private val kotlinLoader = KotlinFileLoader()
    private val kotlinScanner = KotlinScanner()
    private val printer =
        Printer(
            maxLineLength = maxLineLength,
            standardIndent = standardIndentSize,
            continuationIndent = continuationIndentSize
        )

    /**
     * Formats the Kotlin source code in [input] to fit in the column limit [maxLineLength].
     *
     * Returns the formatted source code.
     */
    fun format(input: String): String {
        return printer.print(kotlinScanner.scan(kotlinLoader.parseKotlin(input)))
    }

    /**
     * Formats the Kotlin source file given by [path] to fit in the column limit [maxLineLength],
     * replacing the content of that file with the formatted output.
     */
    fun formatFile(path: Path) {
        Files.writeString(path, format(Files.readString(path)))
    }
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        System.err.println("Usage: KotlinFormatter <file>")
        return
    }
    KotlinFormatter().formatFile(Path.of(args[0]))
}
