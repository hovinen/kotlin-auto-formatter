plugins {
    kotlin("jvm") version "1.3.70"
    groovy
    `java-gradle-plugin`
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
    id("com.gradle.plugin-publish") version "0.12.0"
}

group = "tech.formatter-kt"
version = "0.4"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("gradle-plugin"))
    implementation(gradleApi())
    implementation(localGroovy())
    implementation(project(":formatter"))
}

gradlePlugin {
    plugins {
        create("kotlinFormatterPlugin") {
            id = "tech.formatter-kt.formatter"
            version = "0.4"
            implementationClass = "org.kotlin.formatter.plugin.KotlinFormatterPlugin"
        }
    }
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
