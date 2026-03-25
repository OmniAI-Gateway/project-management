plugins {
    base
}

description = "Root aggregator for composite builds"

val includedBuilds = listOf("OmniAi-SDK", "OmniAiGateway")

fun dependsOnIncluded(taskName: String) =
    includedBuilds.map { gradle.includedBuild(it).task(":$taskName") }

tasks.named("clean") {
    group = "build orchestration"
    description = "Cleans all included builds"
    dependsOn(dependsOnIncluded("clean"))
}

tasks.named("build") {
    group = "build orchestration"
    description = "Builds all included builds"
    dependsOn(dependsOnIncluded("build"))
}

tasks.named("check") {
    group = "verification"
    description = "Runs checks on all included builds"
    dependsOn(dependsOnIncluded("check"))
}
