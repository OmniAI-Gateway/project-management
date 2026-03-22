plugins {
    kotlin("jvm")
}

description = "OpenAI contracts module"

base {
    archivesName.set("contracts-openai")
}

dependencies {
    implementation(project(":core"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(22)
}

tasks.test {
    useJUnitPlatform()
}
