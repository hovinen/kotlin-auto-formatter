plugins {
    kotlin("jvm") version "1.3.70"
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
    jacoco
    application
    `maven-publish`
}

repositories {
    mavenCentral()
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
            xml.isEnabled = false
            csv.isEnabled = false
            html.destination = file("$buildDir/jacoco")
        }
    }
}

application {
    mainClassName = "org.kotlin.formatter.KotlinFormatterKt"
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
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
}
