plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

description = "Gemini outbound adapter module"

base {
    archivesName.set("outbound-gemini")
}

kotlin {
    jvm()
    jvmToolchain(22)

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(project(":contracts:gemini"))
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
