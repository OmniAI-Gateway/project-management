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

        jvmTest.dependencies {
            implementation("com.google.genai:google-genai:1.44.0")
        }
    }
}
