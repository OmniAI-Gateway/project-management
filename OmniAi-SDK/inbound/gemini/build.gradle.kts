plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

description = "Gemini inbound translator module"

base {
    archivesName.set("inbound-gemini")
}

kotlin {
    jvm()
    jvmToolchain(22)

    js(IR) {
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(project(":contracts:gemini"))
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
