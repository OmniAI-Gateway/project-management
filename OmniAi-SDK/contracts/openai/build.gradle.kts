plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

description = "OpenAI contracts module"

base {
    archivesName.set("contracts-openai")
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
