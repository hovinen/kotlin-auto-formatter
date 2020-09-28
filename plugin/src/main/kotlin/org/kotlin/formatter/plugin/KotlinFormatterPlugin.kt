package org.kotlin.formatter.plugin

import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/**
 * A Gradle plugin to add the Kotlin formatter to a project.
 *
 * This registers the following tasks:
 *
 *  * `formatKotlinSource` formats all Kotlin source files, that is, all files in the source set for
 *    the `compileKotlin` task which have the extension `.kt`.
 *  * `formatKotlinScript` formats all Kotlin script files, that is, all files in the project root
 *    directory with the extension `.kts`.
 *  * `formatKotlin` formats all Kotlin files, that is, it invokes both `formatKotlinSource` and
 *    `formatKotlinScript`.
 */
class KotlinFormatterPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val formatKotlinSourceTask =
            target.tasks
                .register("formatKotlinSource", FormatKotlinTask::class.java) {
                    it.group = "formatting"
                    it.description = "Formats all Kotlin source files in this project."
                    it.source(
                        target.extensions
                            .findByType(KotlinProjectExtension::class.java)
                            ?.sourceSets
                            ?.flatMap { set -> set.kotlin.sourceDirectories.files } ?: setOf<File>()
                    )
                }
        val formatKotlinScriptTask =
            target.tasks
                .register("formatKotlinScript", FormatKotlinTask::class.java) {
                    it.group = "formatting"
                    it.description =
                        "Formats all Kotlin script (kts) files in the root directory of this " +
                            "project."
                    it.source(target.fileTree(target.projectDir).apply { include("*.kts") })
                }
        val formatKotlinTask =
            target.tasks
                .register("formatKotlin") {
                    it.group = "formatting"
                    it.description = "Equivalent to formatKotlinSource and formatKotlinScript."
                }
        formatKotlinTask.get().dependsOn(formatKotlinSourceTask.get(), formatKotlinScriptTask.get())
    }
}
