plugins {
    kotlin("jvm")
}

description = "Anthropic contracts module"

base {
    archivesName.set("contracts-anthropic")
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

