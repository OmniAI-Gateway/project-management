plugins {
    kotlin("multiplatform")
}

description = "SDK client"
group = "org.omniai.sdk.client"

kotlin {
    jvm()
    jvmToolchain(22)

    js(IR) {
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(kotlin("stdlib"))
        }
    }
}
