plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "OmniAiGateaway"


include(":inbound")
include(":inbound:web")
include(":outbound")
include(":outbound:ollama")
include(":app")
