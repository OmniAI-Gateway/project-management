plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

description = "OpenAI outbound adapter module"

base {
    archivesName.set("outbound-openai")
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
            implementation(project(":contracts:openai"))
            implementation(libs.kotlinx.serialization.json)
        }


        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
