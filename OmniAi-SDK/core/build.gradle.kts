plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm()
    jvmToolchain(22)

    js(IR) {
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }

        jvmMain.dependencies {
            implementation("com.nimbusds:nimbus-jose-jwt:9.41.2")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
