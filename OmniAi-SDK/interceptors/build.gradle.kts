plugins {
    kotlin("multiplatform")
}

description = "SDK interceptors"
group = "org.omniai.sdk.services"

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

        jvmMain.dependencies {
            implementation("com.nimbusds:nimbus-jose-jwt:9.41.2")
            implementation("org.slf4j:slf4j-api:2.0.12")
        }

        jsMain.dependencies {
            implementation(npm("jose", "5.9.6"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

