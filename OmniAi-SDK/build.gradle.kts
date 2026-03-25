plugins {
    kotlin("jvm") version "2.2.0" apply false
    kotlin("plugin.serialization") version "2.2.0" apply false
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

    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    dependencies {
        add("implementation", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    }

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
    api(project(":contracts:openai"))
    api(project(":contracts:anthropic"))
    api(project(":contracts:gemini"))
    api(project(":outbound:openai"))
    api(project(":outbound:anthropic"))
    api(project(":outbound:gemini"))
}

