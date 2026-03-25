plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

description = "Anthropic contracts module"

base {
    archivesName.set("contracts-anthropic")
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
