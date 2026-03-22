plugins {
    kotlin("jvm")
}

description = "Anthropic outbound adapter module"

base {
    archivesName.set("adapters-anthropic")
}


dependencies {
    implementation(project(":core"))
    implementation(project(":contracts:anthropic"))
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(22)
}

tasks.test {
    useJUnitPlatform()
}

