plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "omniAi-SDK"

include(":core")
include(":contracts")
include(":contracts:anthropic")
include(":contracts:gemini")
include(":contracts:openai")
include(":inbound")
include(":inbound:openai")
include(":inbound:anthropic")
include(":inbound:gemini")
include(":adapters")
include(":adapters:openai")
include(":adapters:anthropic")
include(":adapters:gemini")
