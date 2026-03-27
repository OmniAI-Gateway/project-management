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
            implementation("com.anthropic:anthropic-java:2.18.0")
        }
    }
}
