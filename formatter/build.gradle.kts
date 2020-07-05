import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    jacoco
    application
    id("org.jetbrains.dokka") version "0.10.1"
    `maven-publish`
    id("tech.formatter-kt.formatter") version "0.4.4"
    id("com.github.dawnwords.jacoco.badge") version "0.2.0"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.3.70")
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
    testImplementation("org.assertj:assertj-core:3.11.1")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
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

val dokka by tasks.getting(DokkaTask::class) {
    outputFormat = "html"
    outputDirectory = "$buildDir/dokka"
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("javadoc")
    from(dokka.outputs)
    dependsOn(dokka)
}

val sourcesJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

application {
    mainClassName = "org.kotlin.formatter.KotlinFormatterKt"
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            group = "tech.formatter-kt"
            version = "0.4-SNAPSHOT"
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
