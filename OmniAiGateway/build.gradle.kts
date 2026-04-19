plugins {
    kotlin("jvm") version "2.2.0"
    application
    id("com.gradleup.shadow") version "8.3.6"
}

group = "org.omniai.gateway"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.omniai.sdk:OmniAi-SDK:1.0.0-SNAPSHOT")
    implementation("org.omniai.sdk.gateway.client:gateway-client")
    implementation("org.omniai.sdk.gateway.ktor:gateway-ktor-server")



    implementation(platform("io.ktor:ktor-bom:3.2.3"))
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")

    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    implementation("ch.qos.logback:logback-classic:1.5.3")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(22)
}

application {
    mainClass = "org.omniai.gateway.app.MainKt"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
