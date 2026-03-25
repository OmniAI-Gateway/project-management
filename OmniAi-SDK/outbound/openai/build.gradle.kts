plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

description = "OpenAI outbound adapter module"

base {
    archivesName.set("outbound-openai")
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
            implementation(project(":contracts:openai"))

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        jsMain.dependencies {
            implementation("io.ktor:ktor-client-js:3.2.2")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
