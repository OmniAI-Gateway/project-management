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

    js(IR) {
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(project(":contracts:ktor-http"))
            implementation(project(":contracts:anthropic"))
            implementation(libs.kotlinx.serialization.json)

        }


        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
