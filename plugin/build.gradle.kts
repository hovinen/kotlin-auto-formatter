plugins {
    groovy
    `java-gradle-plugin`
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

sourceSets {
    test {
        runtimeClasspath += configurations.getByName("library")
    }
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    jar {
        from(project(":formatter").tasks["compileKotlin"].outputs)
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
