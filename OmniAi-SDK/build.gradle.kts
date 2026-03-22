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
    api(project(":core"))

    api(project(":inbound:openai"))
    api(project(":inbound:anthropic"))
    api(project(":inbound:gemini"))

    api(project(":adapters:openai"))
    api(project(":adapters:anthropic"))
    api(project(":adapters:gemini"))
}

