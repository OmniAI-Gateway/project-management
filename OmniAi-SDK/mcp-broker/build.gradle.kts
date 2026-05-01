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
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}