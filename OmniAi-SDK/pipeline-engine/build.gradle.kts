plugins {
    kotlin("multiplatform")
}

description = "Gateway pipeline engine"
group = "org.omniai.sdk.pipeline"

kotlin {
    jvm()
    jvmToolchain(22)

    js(IR) {
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
