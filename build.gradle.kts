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
version = "0.4-SNAPSHOT"

subprojects {
    apply<KotlinPlatformJvmPlugin>()
    apply<KtlintPlugin>()
}

project(":plugin") {
    val library: Configuration by configurations.creating

    configurations.getByName("compileClasspath").extendsFrom(library)
    configurations.getByName("testCompileClasspath").extendsFrom(library)
    configurations.getByName("testRuntimeClasspath").extendsFrom(library)

    dependencies {
        library(project(":formatter"))
    }
}
