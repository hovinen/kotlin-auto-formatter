package org.kotlin.formatter.plugin

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
    @TaskAction
    fun format() {
        val kotlinFormatter = KotlinFormatter()
        source.forEach { kotlinFormatter.formatFile(it.toPath()) }
    }
}
