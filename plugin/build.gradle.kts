plugins {
    kotlin("jvm") version "1.3.70"
    groovy
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(gradleApi())
    implementation(localGroovy())
    implementation(project(":formatter"))
}

gradlePlugin {
    plugins {
        create("kotlinFormatterPlugin") {
            id = "org.kotlin.formatter"
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
