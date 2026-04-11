plugins {
    application
    kotlin("jvm")
}

description = "Executable application module"

dependencies {
    implementation(project(":inbound"))
    implementation(project(":outbound"))
    implementation("org.omniai.sdk.gateway:gateway-services:1.0.0-SNAPSHOT")

    implementation(platform("io.ktor:ktor-bom:3.2.3"))

    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-sse")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")

}

application {
    mainClass = "org.omniai.gateway.app.MainKt"
}
