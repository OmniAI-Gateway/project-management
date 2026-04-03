
plugins {
    kotlin("jvm") version "2.2.0" apply false
    base
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
        "implementation"("org.omniai.sdk:OmniAi-SDK:1.0.0-SNAPSHOT")
        "implementation"("org.slf4j:slf4j-api:2.0.12")
        "implementation"("ch.qos.logback:logback-classic:1.5.3")
    }

    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(22)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}


val lifecycleTasks = listOf(
    "clean",
    "assemble",
    "check",
    "build"
)

lifecycleTasks.forEach { taskName ->
    tasks.named(taskName) {
        subprojects.forEach { sub ->
            dependsOn(sub.tasks.matching { it.name == taskName })
        }
    }
}
