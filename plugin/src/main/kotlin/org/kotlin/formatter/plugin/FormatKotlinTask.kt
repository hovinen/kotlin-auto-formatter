package org.kotlin.formatter.plugin

import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.kotlin.formatter.KotlinFormatter

internal open class FormatKotlinTask : SourceTask() {
    @TaskAction
    fun format() {
        val kotlinFormatter = KotlinFormatter()
        source.forEach {
            kotlinFormatter.formatFile(it.toPath())
        }
    }
}
