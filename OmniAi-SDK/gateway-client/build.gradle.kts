plugins {
    kotlin("multiplatform")
}

description = "Gateway client DSL"
group = "org.omniai.sdk.gateway.client"

kotlin {
    jvm()
    jvmToolchain(22)

    js(IR) {
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            api(project(":pipeline-engine"))
            implementation(project(":dispatcher"))
            implementation(project(":interceptors"))
            implementation(project(":inbound:openai"))
            implementation(project(":inbound:anthropic"))
            implementation(project(":inbound:gemini"))
            implementation(project(":contracts:openai"))
            implementation(project(":contracts:anthropic"))
            implementation(project(":contracts:gemini"))
            implementation(project(":http-client"))
            implementation(project(":outbound:openai"))
            implementation(project(":outbound:anthropic"))
            implementation(project(":outbound:gemini"))
            api("io.modelcontextprotocol:kotlin-sdk:0.8.3")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
