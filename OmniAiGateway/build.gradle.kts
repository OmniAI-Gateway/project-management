
plugins {
    kotlin("jvm") version "2.2.0" apply false
}

group = "org.omniai.gateway"
version = "1.0.0-SNAPSHOT"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    dependencies {
        "testImplementation"(kotlin("test"))
        "implementation"("org.omniai.sdk:omniAi-SDK:1.0.0-SNAPSHOT")

    }

    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(22)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}