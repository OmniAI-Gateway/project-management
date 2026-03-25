plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

description = "OpenAI inbound translator module"

base {
    archivesName.set("inbound-openai")
}

kotlin {
    jvm()
    jvmToolchain(22)

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(project(":contracts:openai"))
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
