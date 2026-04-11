plugins {
    kotlin("jvm")
}

description = "Gateway interceptors inside SDK"

base {
    archivesName.set("gateway-interceptors")
}

dependencies {
    implementation(project(":core"))
    implementation("org.slf4j:slf4j-api:2.0.12")
}

kotlin {
    jvmToolchain(22)
}

