plugins {
    kotlin("jvm")
}

description = "Gateway services inside SDK"

base {
    archivesName.set("gateway-services")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":gateway:interceptors"))
}

kotlin {
    jvmToolchain(22)
}

