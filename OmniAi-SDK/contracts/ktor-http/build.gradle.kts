plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

description = "Ktor HTTP transport contracts module"

base {
    archivesName.set("contracts-ktor-http")
}

kotlin {
    jvm()
    jvmToolchain(22)

    js(IR) {
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        jsMain.dependencies {
            implementation(libs.ktor.client.js)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

