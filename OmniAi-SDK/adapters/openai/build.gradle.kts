plugins {
    kotlin("jvm")
}

description = "OpenAI outbound adapter module"

base {
    archivesName.set("adapters-openai")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":contracts:openai"))
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(22)
}

tasks.test {
    useJUnitPlatform()
}

