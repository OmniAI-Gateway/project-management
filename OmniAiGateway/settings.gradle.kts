plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "OmniAiGateway"

includeBuild("../OmniAi-SDK")

include(":inbound")
include(":outbound")
include(":app")
