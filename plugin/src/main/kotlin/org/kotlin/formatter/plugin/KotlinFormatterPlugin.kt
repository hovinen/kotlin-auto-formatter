package org.kotlin.formatter.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class KotlinFormatterPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.tasks.register("formatKotlin", FormatKotlinTask::class.java)
    }
}
