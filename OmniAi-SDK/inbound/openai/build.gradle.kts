plugins {
    kotlin("jvm")
}

description = "OpenAI inbound translator module"

base {
    archivesName.set("inbound-openai")
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

