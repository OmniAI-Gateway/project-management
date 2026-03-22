plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation(kotlin("test"))
    api(libs.kotlinx.coroutines.core)

}

kotlin {
    jvmToolchain(22)
}

tasks.test {
    useJUnitPlatform()
}

