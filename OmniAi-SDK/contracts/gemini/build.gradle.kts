plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

description = "Gemini contracts module"

base {
    archivesName.set("contracts-gemini")
}

kotlin {
    jvm()
    jvmToolchain(22)

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
