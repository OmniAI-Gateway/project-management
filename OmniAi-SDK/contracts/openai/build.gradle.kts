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

    js(IR) {
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
