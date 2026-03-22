plugins {
    kotlin("jvm")
}

description = "Gemini inbound translator module"

base {
    archivesName.set("inbound-gemini")
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

