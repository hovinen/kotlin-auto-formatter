import java.io.ByteArrayOutputStream
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin
import org.jlleitschuh.gradle.ktlint.KtlintPlugin

buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.72")
        classpath("org.jlleitschuh.gradle:ktlint-gradle:9.2.1")
    }
}

group = "tech.formatter-kt"
version = "${gitVersion()}-SNAPSHOT"

subprojects {
    apply<KotlinPlatformJvmPlugin>()
    apply<KtlintPlugin>()
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
    ByteArrayOutputStream().use { stream ->
        exec {
            commandLine("git", "describe", "--tags")
            standardOutput = stream
        }
        val tagName = stream.toString(Charsets.UTF_8).trim()
        val match = versionRegex.matchEntire(tagName)
        return if (match != null) match.groupValues[1] else default
    }
}
