import java.io.ByteArrayOutputStream
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    jacoco
    application
    id("org.jetbrains.dokka") version "0.10.1"
    `maven-publish`
    id("com.github.dawnwords.jacoco.badge") version "0.2.0"
    java
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.4.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
    testImplementation("org.assertj:assertj-core:3.11.1")
}

tasks {
    compileKotlin { kotlinOptions.jvmTarget = "1.8" }
    compileTestKotlin { kotlinOptions.jvmTarget = "1.8" }
    test {
        useJUnitPlatform()
        finalizedBy(jacocoTestReport) // report is always generated after tests run
    }
    jacocoTestReport {
        dependsOn(test) // tests are required to run before generating the report
        reports {
            xml.isEnabled = true
            csv.isEnabled = false
            html.destination = file("$buildDir/jacoco")
        }
    }
}

val dokka by
    tasks.getting(DokkaTask::class) {
        outputFormat = "html"
        outputDirectory = "$buildDir/dokka"
    }

val dokkaJar by
    tasks.creating(Jar::class) {
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        archiveClassifier.set("javadoc")
        from(dokka.outputs)
        dependsOn(dokka)
    }

val sourcesJar by
    tasks.creating(Jar::class) {
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

application { mainClassName = "org.kotlin.formatter.KotlinFormatterKt" }

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            group = "tech.formatter-kt"
            version = "${gitVersion()}-SNAPSHOT"
            artifact(dokkaJar)
            artifact(sourcesJar)
            pom {
                name.set("Kotlin autoformatter")
                description.set("An automated opinionated formatter for Kotlin")
                url.set("https://github.com/hovinen/kotlin-auto-formatter")
                licenses {
                    license {
                        name.set("GNU General Public License 3")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.html")
                    }
                }
                developers {
                    developer {
                        id.set("hovinen")
                        name.set("Bradford Hovinen")
                        email.set("hovinen@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/hovinen/kotlin-auto-formatter.git")
                    developerConnection.set(
                        "scm:git:https://github.com/hovinen/kotlin-auto-formatter.git"
                    )
                    url.set("https://github.com/hovinen/kotlin-auto-formatter")
                }
            }
        }
    }
    repositories {
        maven {
            name = "sonatype"
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
            credentials {
                val mavenUser: String? by project
                val mavenPassword: String? by project
                username = mavenUser
                password = mavenPassword
            }
        }
    }
}

jacocoBadgeGenSetting {
    jacocoReportPath = "formatter/build/reports/jacoco/test/jacocoTestReport.xml"
    readmePath = "README.md"
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
