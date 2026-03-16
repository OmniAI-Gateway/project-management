plugins {
    kotlin("jvm") version "2.3.0" apply false
    id("java-library")

}

group = "org.omniaigateway"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
}

subprojects {
    group = rootProject.group
    version = rootProject.version

    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(22)
    }

}

dependencies {
    api(project(":domain"))
}

