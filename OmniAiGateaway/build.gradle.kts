plugins {
    kotlin("jvm") version "2.3.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.example:OmniAiGateaway-Core:1.0-SNAPSHOT")
}

kotlin {
    jvmToolchain(22)
}

tasks.test {
    useJUnitPlatform()
}