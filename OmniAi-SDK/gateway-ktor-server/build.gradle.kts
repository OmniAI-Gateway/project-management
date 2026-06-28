plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

description = "Gateway Ktor server transport adapter"
group = "org.omniai.sdk.gateway.ktor"

dependencies {
    implementation(project(":gateway-client"))
    implementation(project(":core"))
    implementation(project(":inbound:openai"))
    implementation(project(":inbound:anthropic"))
    implementation(project(":inbound:gemini"))
    implementation(project(":contracts:openai"))
    implementation(project(":contracts:anthropic"))
    implementation(project(":contracts:gemini"))
    implementation(project(":http-client"))

    implementation(libs.ktor.server.core)
    implementation(libs.kotlinx.serialization.json)
}

kotlin {
    jvmToolchain(22)
}
