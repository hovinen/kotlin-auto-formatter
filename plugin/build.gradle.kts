import java.io.ByteArrayOutputStream

plugins {
    groovy
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "0.12.0"
    id("tech.formatter-kt.formatter") version "0.6.6"
}

group = "tech.formatter-kt"
version = gitVersion()

repositories { mavenCentral() }

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("gradle-plugin"))
    implementation(gradleApi())
    implementation(localGroovy())

    testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
    testImplementation("org.assertj:assertj-core:3.11.1")
}

gradlePlugin {
    plugins {
        create("kotlinFormatterPlugin") {
            id = "tech.formatter-kt.formatter"
            version = project.version
            implementationClass = "org.kotlin.formatter.plugin.KotlinFormatterPlugin"
        }
    }
}

tasks {
    compileKotlin { kotlinOptions.jvmTarget = "1.8" }
    compileTestKotlin { kotlinOptions.jvmTarget = "1.8" }
    jar { from(project(":formatter").tasks["compileKotlin"].outputs) }
    test { useJUnitPlatform() }
    pluginUnderTestMetadata {
        pluginClasspath.setFrom(
            sourceSets.main.get().runtimeClasspath,
            configurations.compileClasspath
        )
    }
}

pluginBundle {
    website = "https://github.com/hovinen/kotlin-auto-formatter"
    vcsUrl = "https://github.com/hovinen/kotlin-auto-formatter"

    description = "Automatically formats Kotlin code"

    (plugins) {
        "kotlinFormatterPlugin" {
            // id is captured from java-gradle-plugin configuration
            displayName = "Kotlin autoformatter"
            tags = listOf("kotlin", "formatting")
        }
    }
}

fun gitVersion(default: String = "0.0.0"): String {
    val versionRegex = Regex("v(\\d+\\.\\d+\\.\\d+)(-\\d+-\\w+)?")
    val tagName: String = System.getenv("TAG_NAME") ?: tagNameFromGit()
    val match = versionRegex.matchEntire(tagName)
    return if (match != null) match.groupValues[1] else default
}

fun tagNameFromGit(): String {
    ByteArrayOutputStream()
        .use { stream ->
            exec {
                commandLine("git", "describe", "--tags")
                standardOutput = stream
            }
            return stream.toString(Charsets.UTF_8).trim()
        }
}
