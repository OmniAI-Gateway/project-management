package org.omniai.gateway.app

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json
import org.omniai.gateway.inbound.web.GatewayInboundAdapters
import org.omniai.gateway.inbound.web.installWebRoutes
import org.omniai.gateway.outbound.builder.GatewayOutboundBuilder
import org.omniai.gateway.services.gatewayServiceAssembler
import org.omniai.sdk.adapters.anthropic.AnthropicOutboundAdapter
import org.omniai.sdk.adapters.gemini.GeminiOutboundAdapter
import org.omniai.sdk.adapters.openai.OpenAiOutboundAdapter
import org.omniai.sdk.core.ports.OutboundPort
import org.omniai.sdk.inbound.anthropic.AnthropicInboundAdapter
import org.omniai.sdk.inbound.gemini.GeminiInboundAdapter
import org.omniai.sdk.inbound.openai.OpenAiInboundAdapter

private data class GatewayConfig(
    val port: Int,
    val openAiApiKey: String,
    val openAiModel: String,
    val openAiBaseUrl: String,
    val geminiApiKey: String,
    val geminiModel: String,
    val geminiBaseUrl: String,
    val anthropicApiKey: String?,
    val anthropicModel: String,
    val anthropicBaseUrl: String
)

suspend fun main() {
    val config = loadGatewayConfig()
    val jsonConfig = buildJsonConfig()
    val outbounds = buildOutbounds(config)

    //mudar aqui depois tb para receber as coisas bemm
    val service = gatewayServiceAssembler(outbounds = outbounds, configSource = null, httpClient = null, interceptors = null)

    val adapters = GatewayInboundAdapters(
        anthropic = AnthropicInboundAdapter(service),
        openAi = OpenAiInboundAdapter(service),
        gemini = GeminiInboundAdapter(service)
    )

    startServer(
        port = config.port,
        jsonConfig = jsonConfig,
        adapters = adapters
    )
}

private fun loadGatewayConfig(): GatewayConfig =
    GatewayConfig(
        port = (System.getenv("PORT") ?: "1900").toInt(),
        openAiApiKey = requireEnv("OPENAI_API_KEY"),
        openAiModel = System.getenv("OPENAI_MODEL") ?: "llama-3.3-70b-versatile",
        openAiBaseUrl = System.getenv("OPENAI_BASE_URL") ?: "https://api.groq.com/openai/v1",
        geminiApiKey = requireEnv("GEMINI_API_KEY"),
        geminiModel = System.getenv("GEMINI_MODEL") ?: "gemini-2.5-flash:generateContent",
        geminiBaseUrl = System.getenv("GEMINI_BASE_URL") ?: "https://generativelanguage.googleapis.com/v1beta",
        anthropicApiKey = System.getenv("ANTHROPIC_API_KEY"),
        anthropicModel = System.getenv("ANTHROPIC_MODEL") ?: "claude-3-7-sonnet-latest",
        anthropicBaseUrl = System.getenv("ANTHROPIC_BASE_URL") ?: "https://api.anthropic.com/v1"
    )

private fun buildJsonConfig(): Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

private fun buildOutbounds(config: GatewayConfig): List<OutboundPort> {
    val builder = GatewayOutboundBuilder()
    val inputs = mutableListOf(
        builder.fromModelUrlPairs(
            outboundClass = OpenAiOutboundAdapter::class,
            apiKey = config.openAiApiKey,
            modelUrlPairs = listOf(config.openAiModel to config.openAiBaseUrl)
        ),
        builder.fromModelUrlPairs(
            outboundClass = GeminiOutboundAdapter::class,
            apiKey = config.geminiApiKey,
            modelUrlPairs = listOf(config.geminiModel to config.geminiBaseUrl)
        )
    )

    if (!config.anthropicApiKey.isNullOrBlank()) {
        inputs += builder.fromModelUrlPairs(
            outboundClass = AnthropicOutboundAdapter::class,
            apiKey = config.anthropicApiKey,
            modelUrlPairs = listOf(config.anthropicModel to config.anthropicBaseUrl)
        )
    }

    return builder.build(inputs)
}

private fun startServer(
    port: Int,
    jsonConfig: Json,
    adapters: GatewayInboundAdapters
) {
    embeddedServer(Netty, port = port) {
        configureHttp(jsonConfig)
        installWebRoutes(jsonConfig, adapters)
    }.start(wait = true)
}

private fun Application.configureHttp(jsonConfig: Json) {
    install(ContentNegotiation) {
        json(jsonConfig)
    }
}

private fun requireEnv(name: String): String =
    System.getenv(name) ?: throw IllegalStateException("Environment variable '$name' is required")
