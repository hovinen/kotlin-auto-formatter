package org.kotlin.formatter.plugin

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.kotlin.formatter.KotlinFormatter
import java.util.concurrent.Callable

internal open class FormatKotlinTask : SourceTask() {
    @TaskAction
    fun format() {
        val kotlinFormatter = KotlinFormatter()
        source.forEach {
            kotlinFormatter.formatFile(it.toPath())
        }
    }
}
