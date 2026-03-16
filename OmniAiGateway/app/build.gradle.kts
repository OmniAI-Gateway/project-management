plugins {
    application
    kotlin("jvm")
}

description = "Executable application module"

dependencies {
    implementation(project(":inbound:web"))
    implementation(project(":outbound:ollama"))
}

application {
    mainClass = "org.omniaigateway.app.MainKt"
}
