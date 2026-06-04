plugins {
    kotlin("multiplatform")
}

description = "SDK services"
group = "org.omniai.sdk.services"

kotlin {
    jvm()
    jvmToolchain(22)

    js(IR) {
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(project(":pipeline-engine"))
            implementation(project(":interceptors"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
