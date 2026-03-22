plugins {
    kotlin("jvm")
}

description = "Gemini contracts module"

base {
    archivesName.set("contracts-gemini")
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(22)
}

tasks.test {
    useJUnitPlatform()
}

