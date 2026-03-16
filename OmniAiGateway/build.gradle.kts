
plugins {
    kotlin("jvm") version "2.3.0" apply false
}

group = "org.omniaigateway"
version = "1.0-SNAPSHOT"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    dependencies {
        "testImplementation"(kotlin("test"))
        "implementation"("org.omniaigateway:OmniAiGateway-Core:1.0-SNAPSHOT")

    }

    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(22)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}