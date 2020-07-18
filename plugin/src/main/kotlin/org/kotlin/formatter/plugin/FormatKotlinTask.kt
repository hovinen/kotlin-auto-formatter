package org.kotlin.formatter.plugin

import java.nio.file.FileSystems
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.kotlin.formatter.KotlinFormatter

/**
 * A Gradle task to format Kotlin source files.
 *
 * This runs [KotlinFormatter] against all source files specified for the task, replacing their
 * existing content with the formatted output.
 */
internal open class FormatKotlinTask : SourceTask() {
    private val pathMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.{kt,kts}")

    @TaskAction
    fun format() {
        val kotlinFormatter = KotlinFormatter()
        source.map { it.toPath() }
            .filter { pathMatcher.matches(it) }
            .forEach { kotlinFormatter.formatFile(it) }
    }
}
