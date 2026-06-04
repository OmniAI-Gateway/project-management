plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
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
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
            api(project(":core"))
            api(project(":pipeline-engine"))
            implementation(libs.kotlinx.serialization.json)

        }

        jvmMain.dependencies {
            implementation("com.nimbusds:nimbus-jose-jwt:9.41.2")
            implementation("org.slf4j:slf4j-api:2.0.12")

            api(project.dependencies.platform("io.opentelemetry:opentelemetry-bom:1.38.0"))
            api(project.dependencies.platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:2.4.0-alpha"))
            api("io.opentelemetry:opentelemetry-api")
            implementation("io.opentelemetry:opentelemetry-extension-kotlin")

        }

        jsMain.dependencies {
            implementation(npm("jose", "5.9.6"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

