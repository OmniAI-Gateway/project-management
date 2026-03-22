plugins {
    kotlin("jvm")
}

description = "Anthropic inbound translator module"


base {
    archivesName.set("inbound-anthropic")
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

