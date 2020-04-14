package org.kotlin.formatter.plugin

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.kotlin.formatter.KotlinFormatter
import java.util.concurrent.Callable

internal class FormatKotlinTask : SourceTask() {
    @get:InputFiles
    internal val sources: FileCollection = project.files(Callable { return@Callable source })

    @TaskAction
    fun format() {
        val kotlinFormatter = KotlinFormatter()
        sources.files.forEach {
            kotlinFormatter.formatFile(it.toPath())
        }
    }
}
