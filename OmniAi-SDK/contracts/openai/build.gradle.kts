plugins {
    kotlin("jvm")
}

description = "OpenAI contracts module"

base {
    archivesName.set("contracts-openai")
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
