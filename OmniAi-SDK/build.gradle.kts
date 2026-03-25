plugins {
    kotlin("multiplatform") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
}

group = "org.omniai.sdk"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

subprojects {
    version = rootProject.version

    repositories {
        mavenCentral()
    }
}

kotlin {
    jvm()
    jvmToolchain(22)

    sourceSets {
        commonMain.dependencies {
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

            implementation("io.ktor:ktor-client-core:3.2.2")
            implementation("io.ktor:ktor-client-content-negotiation:3.2.2")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.2.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
        }

        jvmMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:3.2.2")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

