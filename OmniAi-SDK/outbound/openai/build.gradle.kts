plugins {
    kotlin("jvm")
}

description = "OpenAI outbound adapter module"

base {
    archivesName.set("outbound-openai")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":contracts:openai"))

    implementation(project(":core"))
    implementation(project(":contracts:openai"))

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    testImplementation(kotlin("test"))

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(22)
}

tasks.test {
    useJUnitPlatform()
}

