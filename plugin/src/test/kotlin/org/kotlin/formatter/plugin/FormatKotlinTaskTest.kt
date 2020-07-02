package org.kotlin.formatter.plugin

import java.nio.file.Files
import java.nio.file.Path
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GFileUtils.parentMkdirs
import org.gradle.util.GFileUtils.writeFile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FormatKotlinTaskTest {
    @TempDir
    lateinit var testFolder: Path

    @BeforeEach
    fun setup() {
        writeBuildScript()
        writeSettingsFile()
    }

    private fun writeBuildScript() {
        writeFile(
            """
                plugins {
                    kotlin("jvm") version "1.3.72"
                    id("tech.formatter-kt.formatter")
                }
            """.trimIndent(),
            testFolder.resolve("build.gradle.kts").toFile()
        )
    }

    private fun writeSettingsFile() {
        writeFile(
            """rootProject.name = "test"""".trimIndent(),
            testFolder.resolve("settings.gradle.kts").toFile()
        )
    }

    @Test
    fun `should format Kotlin source files`() {
        val sourceDirectory = testFolder.resolve("src/main/kotlin/somepackage")
        val sourceFile = sourceDirectory.resolve("AClass.kt")
        parentMkdirs(sourceDirectory.toFile())
        writeFile(
            """
                package somepackage
                
                class  AClass {
                val aProperty:
                String = "Hello"
                }
            """.trimIndent(),
            sourceFile.toFile()
        )

        runFormatter()

        assertThat(Files.readString(sourceFile))
            .isEqualTo(
                """
                    package somepackage
                    
                    class AClass {
                        val aProperty: String = "Hello"
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `should not format Java source files`() {
        val sourceDirectory = testFolder.resolve("src/main/kotlin/somepackage")
        writeKotlinSourceFile("AnotherClass", sourceDirectory)
        val sourceFile = sourceDirectory.resolve("AClass.java")
        parentMkdirs(sourceDirectory.toFile())
        writeFile(
            """
                package somepackage;
                public class  AClass { String aProperty =
                "Hello"; }
            """.trimIndent(),
            sourceFile.toFile()
        )

        val result = runFormatter()

        assertThat(result.task(":formatKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(Files.readString(sourceFile))
            .isEqualTo(
                """
                    package somepackage;
                    public class  AClass { String aProperty =
                    "Hello"; }
                """.trimIndent()
            )
    }

    private fun writeKotlinSourceFile(className: String, sourceDirectory: Path) {
        val sourceFile = sourceDirectory.resolve("$className.kt")
        writeFile(
            """
                package somepackage
                
                class $className
            """.trimIndent(),
            sourceFile.toFile()
        )
    }

    private fun runFormatter(): BuildResult {
        return GradleRunner.create()
            .withProjectDir(testFolder.toFile())
            .withPluginClasspath()
            .withArguments("formatKotlin")
            .forwardOutput()
            .build()
    }
}
