plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "omniAi-SDK"

include(":core")
include(":contracts")
include(":contracts:anthropic")
include(":contracts:gemini")
include(":contracts:ktor-http")
include(":contracts:openai")
include(":inbound")
include(":inbound:openai")
include(":inbound:anthropic")
include(":inbound:gemini")
include(":outbound")
include(":outbound:openai")
include(":outbound:anthropic")
include(":outbound:gemini")
