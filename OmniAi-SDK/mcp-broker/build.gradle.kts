plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

description = "SDK mcp-broker"
group = "org.omniai.sdk.services"

val mcp_version = "0.8.3"

kotlin {
    jvm()
    jvmToolchain(22)

    js(IR) {
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            implementation("io.modelcontextprotocol:kotlin-sdk:$mcp_version")
            implementation("net.mamoe.yamlkt:yamlkt:0.13.0")
            implementation(project(":core"))
            implementation(project(":http-client"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        jsMain.dependencies {
            implementation(libs.ktor.client.js)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}