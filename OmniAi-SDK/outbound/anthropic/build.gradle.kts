plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

description = "Anthropic outbound adapter module"

base {
    archivesName.set("outbound-anthropic")
}


kotlin {
    jvm()
    jvmToolchain(22)

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(project(":contracts:anthropic"))
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
