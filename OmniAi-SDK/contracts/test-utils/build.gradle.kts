plugins {
    kotlin("multiplatform")
}

description = "Test Utilities for OmniAi SDK"

kotlin {
    jvm()
    jvmToolchain(22)

    js(IR) {
        nodejs()
    }
}
