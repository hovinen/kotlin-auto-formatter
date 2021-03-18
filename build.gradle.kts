import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin
import org.jlleitschuh.gradle.ktlint.KtlintPlugin
import org.kotlin.formatter.plugin.KotlinFormatterPlugin
import java.io.ByteArrayOutputStream

plugins {
    kotlin("jvm") version "1.3.72" apply false
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1" apply false
    id("tech.formatter-kt.formatter") version "0.7.5" apply false
}

group = "tech.formatter-kt"
version = "${gitVersion()}-SNAPSHOT"

subprojects {
    apply<KotlinPlatformJvmPlugin>()
    apply<KtlintPlugin>()
    apply<KotlinFormatterPlugin>()
}

project(":plugin") {
    val library: Configuration by configurations.creating

    configurations.getByName("compileClasspath").extendsFrom(library)
    configurations.getByName("testRuntimeClasspath").extendsFrom(library)

    dependencies {
        library(project(":formatter"))
    }
}

fun gitVersion(default: String = "0.0.0"): String {
    val versionRegex = Regex("v(\\d+\\.\\d+\\.\\d+)(-\\d+-\\w+)?")
    val tagName: String = System.getenv("TAG_NAME") ?: tagNameFromGit()
    val match = versionRegex.matchEntire(tagName)
    return if (match != null) match.groupValues[1] else default
}

fun tagNameFromGit(): String {
    ByteArrayOutputStream().use { stream ->
        exec {
            commandLine("git", "describe", "--tags")
            standardOutput = stream
        }
        return stream.toString(Charsets.UTF_8).trim()
    }
}
