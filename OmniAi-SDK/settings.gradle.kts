plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "OmniAi-SDK"


include(":core")
include(":pipeline-engine")
include(":contracts")
include(":contracts:anthropic")
include(":contracts:gemini")
include(":http-client")
include(":contracts:openai")
include(":inbound")
include(":inbound:openai")
include(":inbound:anthropic")
include(":inbound:gemini")
include(":outbound")
include(":outbound:openai")
include(":outbound:anthropic")
include(":outbound:gemini")
include(":interceptors")
include(":mcp-broker")
include(":dispatcher")
include(":gateway-client")
include(":gateway-ktor-server")
