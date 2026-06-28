plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

description = "SDK mcp-broker"
group = "org.omniai.sdk.services"

val mcpVersion = "0.13.0"

kotlin {
    jvm()
    jvmToolchain(22)

    js(IR) {
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            implementation("io.modelcontextprotocol:kotlin-sdk:$mcpVersion")
            implementation("io.modelcontextprotocol:kotlin-sdk-server:$mcpVersion")
            implementation("net.mamoe.yamlkt:yamlkt:0.13.0")
            implementation(project(":core"))
            implementation(project(":http-client"))
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.cio)
            implementation(libs.ktor.server.sse)
            implementation(libs.ktor.server.websockets)
            implementation(libs.ktor.server.cors)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.websockets)
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
