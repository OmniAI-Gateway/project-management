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
            implementation(project(":interceptors"))
            implementation(project(":inbound:openai"))
            implementation(project(":inbound:anthropic"))
            implementation(project(":inbound:gemini"))
            implementation(project(":contracts:openai"))
            implementation(project(":contracts:anthropic"))
            implementation(project(":contracts:gemini"))
        }

        jvmMain.dependencies {
            implementation(project(":dispatcher"))
            implementation(project(":interceptors"))
            implementation(project(":contracts:ktor-http"))
            implementation(project(":outbound:openai"))
            implementation(project(":outbound:anthropic"))
            implementation(project(":outbound:gemini"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

