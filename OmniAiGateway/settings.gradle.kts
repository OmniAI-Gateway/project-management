plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "OmniAiGateway"

includeBuild("../OmniAi-SDK") {
    dependencySubstitution {
        substitute(module("org.omniai.sdk:OmniAi-SDK"))
            .using(project(":"))
        substitute(module("org.omniai.sdk.gateway:gateway-services"))
            .using(project(":gateway:services"))
        substitute(module("org.omniai.sdk.gateway:gateway-interceptors"))
            .using(project(":gateway:interceptors"))
    }
}
include(":inbound")
include(":inbound:web")
include(":outbound")
include(":outbound:builder")
include(":app")
