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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FormatKotlinTaskTest {
    @TempDir
    lateinit var testFolder: Path

    @BeforeEach
    fun writeSettingsFile() {
        writeFile(
            """rootProject.name = "test"""".trimIndent(),
            testFolder.resolve("settings.gradle.kts").toFile()
        )
    }

    @Nested
    inner class WithKotlinPlugin {
        @BeforeEach
        fun writeBuildScript() {
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

            runFormatter("formatKotlinSource")

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

            val result = runFormatter("formatKotlinSource")

            assertThat(result.task(":formatKotlinSource")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
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
    }

    @Nested
    inner class WithoutKotlinPlugin {
        @Test
        fun `should skip task if Kotlin plugin was not applied`() {
            writeFile(
                """
                    plugins {
                        java
                        id("tech.formatter-kt.formatter")
                    }
                """.trimIndent(),
                testFolder.resolve("build.gradle.kts").toFile()
            )

            val result = runFormatter("formatKotlinSource")

            assertThat(result.task(":formatKotlinSource")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
        }
    }

    @Nested
    inner class FormattingBuildScript {
        @Test
        fun `formats Kotlin DSL build scripts`() {
            val buildFile = testFolder.resolve("build.gradle.kts")
            writeFile(
                """
                    plugins { java
                        id("tech.formatter-kt.formatter")
                    }
                """.trimIndent(),
                buildFile.toFile()
            )

            val result = runFormatter("formatKotlinScript")

            assertThat(result.task(":formatKotlinScript")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(Files.readString(buildFile))
                .isEqualTo(
                    """
                        plugins {
                            java
                            id("tech.formatter-kt.formatter")
                        }
                    """.trimIndent()
                )
        }

        @Test
        fun `does not format Groovy DSL build scripts`() {
            val buildFile = testFolder.resolve("build.gradle")
            writeFile(
                """
                    plugins { id("tech.formatter-kt.formatter")
                    }
                """.trimIndent(),
                buildFile.toFile()
            )

            runFormatter("formatKotlinScript")

            assertThat(Files.readString(buildFile))
                .isEqualTo(
                    """
                        plugins { id("tech.formatter-kt.formatter")
                        }
                    """.trimIndent()
                )
        }
    }

    @Nested
    inner class FormattingAllSources {
        @Test
        fun `formats both sources and scripts with formatKotlin task`() {
            val buildFile = testFolder.resolve("build.gradle.kts")
            writeFile(
                """
                    plugins { kotlin("jvm") version "1.3.72"
                        id("tech.formatter-kt.formatter")
                    }
                """.trimIndent(),
                buildFile.toFile()
            )
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

            runFormatter("formatKotlin")

            assertThat(Files.readString(buildFile))
                .isEqualTo(
                    """
                        plugins {
                            kotlin("jvm") version "1.3.72"
                            id("tech.formatter-kt.formatter")
                        }
                    """.trimIndent()
                )
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
    }

    private fun runFormatter(task: String): BuildResult {
        return GradleRunner.create()
            .withProjectDir(testFolder.toFile())
            .withPluginClasspath()
            .withArguments(task)
            .forwardOutput()
            .build()
    }
}
