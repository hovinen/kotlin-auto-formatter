package org.kotlin.formatter.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

class KotlinFormatterPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.tasks.register("formatKotlin", FormatKotlinTask::class.java) {
            it.source(target.extensions.getByType(KotlinProjectExtension::class.java).sourceSets.flatMap { set -> set.kotlin.sourceDirectories.files })
        }
    }
}
