plugins {
    kotlin("jvm") version "1.3.70"
    groovy
    `java-gradle-plugin`
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
}

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
            id = "org.kotlin.formatter"
            version = "0.0.1"
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
