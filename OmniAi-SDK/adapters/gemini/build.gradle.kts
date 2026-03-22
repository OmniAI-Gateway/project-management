plugins {
    kotlin("jvm")
}

description = "Gemini outbound adapter module"

base {
    archivesName.set("adapters-gemini")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":contracts:gemini"))
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(22)
}

tasks.test {
    useJUnitPlatform()
}

