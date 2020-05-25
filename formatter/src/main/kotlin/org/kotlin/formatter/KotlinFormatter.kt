package org.kotlin.formatter

import java.nio.file.Path
import java.nio.file.Paths
import org.jetbrains.kotlin.utils.addToStdlib.indexOfOrNull
import org.kotlin.formatter.loading.KotlinFileLoader
import org.kotlin.formatter.output.Printer
import org.kotlin.formatter.output.TokenPreprocessor
import org.kotlin.formatter.scanning.KotlinScanner
import org.kotlin.formatter.scanning.nodepattern.NodeSequenceNotMatchedException

/**
 * Automatic formatter for Kotlin code to fit in a given column limit.
 *
 * @property maxLineLength the maximum column limit in which to try to fit the code
 * @property continuationIndentSize the additional amount to indent when introducing a line break
 *     within an existing statement
 */
class KotlinFormatter(
    private val maxLineLength: Int = 100,
    private val standardIndentSize: Int = 4,
    private val continuationIndentSize: Int = 4
) {
    private val kotlinLoader = KotlinFileLoader()

    /**
     * Formats the Kotlin source code in [input] to fit in the column limit [maxLineLength].
     *
     * Returns the formatted source code.
     */
    fun format(input: String): String {
        val kotlinScanner = KotlinScanner()
        val tokenPreprocessor = TokenPreprocessor()
        val printer =
            Printer(
                maxLineLength = maxLineLength,
                standardIndent = standardIndentSize,
                continuationIndent = continuationIndentSize
            )
        return printer.print(
            tokenPreprocessor.preprocess(kotlinScanner.scan(kotlinLoader.parseKotlin(input)))
        )
    }

    /**
     * Formats the Kotlin source file given by [path] to fit in the column limit [maxLineLength],
     * replacing the content of that file with the formatted output.
     */
    fun formatFile(path: Path) {
        val input = path.toFile().readText(Charsets.UTF_8)
        try {
            path.toFile().writeText(format(input), Charsets.UTF_8)
        } catch (thrown: NodeSequenceNotMatchedException) {
            println(
                "Could not process $path " +
                    "(line ${lineNumber(input, thrown.nodes.firstOrNull()?.startOffset)}): " +
                    thrown.message
            )
        }
    }

    private fun lineNumber(input: String, offset: Int?): Int =
        if (offset != null) {
            val lineOffsets = generateSequence(-1) { input.indexOfOrNull('\n', it + 1) }.toList()
            val index = lineOffsets.indexOfFirst { it > offset }
            if (index == -1) lineOffsets.size else index
        } else {
            0
        }
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        System.err.println("Usage: KotlinFormatter <file>")
        return
    }
    KotlinFormatter().formatFile(Paths.get(args[0]))
}
