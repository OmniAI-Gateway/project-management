plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "OmniAiGateway"


include(":inbound")
include(":inbound:web")
include(":outbound")
include(":outbound:ollama")
include(":app")


includeBuild("../OmniAi-SDK")
