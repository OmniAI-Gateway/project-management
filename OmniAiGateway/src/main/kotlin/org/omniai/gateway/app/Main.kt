package org.omniai.gateway.app

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.omniai.sdk.adapters.anthropic.AnthropicOutboundAdapter
import org.omniai.sdk.adapters.gemini.GeminiOutboundAdapter
import org.omniai.sdk.adapters.openai.OpenAiOutboundAdapter
import org.omniai.sdk.core.http.KtorHttpTransportClient
import org.omniai.sdk.gateway.client.assemble
import org.omniai.sdk.gateway.client.gatewayConfig
import org.omniai.sdk.gateway.client.outbound.OutboundTarget
import org.omniai.sdk.gateway.client.outbound.buildOutbounds
import org.omniai.sdk.gateway.ktor.installAiGateway

private data class GatewayConfig(
    val port: Int,
//    val openAiApiKey: String,
//    val openAiModel: String,
//    val openAiBaseUrl: String,
    val geminiApiKey: String,
    val geminiModel: String,
    val geminiBaseUrl: String,
//    val anthropicApiKey: String?,
//    val anthropicModel: String,
//    val anthropicBaseUrl: String,
)

suspend fun main() {
    val config = loadGatewayConfig()
    val jsonConfig = buildJsonConfig()
    val outbounds = buildOutbounds(gatewayOutbounds(config))

    val definition = gatewayConfig {
        outbounds {
            outbounds.forEach { outbound -> +outbound }
        }

        authorizationServer {
            none()
        }

        services {
            builtIn()
        }
    }

    val runtime = definition.assemble(httpClient = KtorHttpTransportClient.default())

    embeddedServer(Netty, port = config.port) {
        configureHttp(jsonConfig)
        routing {
            installAiGateway(runtime) {
                json = jsonConfig
            }
        }
    }.start(wait = true)
}

private fun loadGatewayConfig(): GatewayConfig =
    ConfigFactory.load().let { appConfig ->
        GatewayConfig(
            port = System.getenv("PORT")?.toIntOrNull() ?: appConfig.safeInt("gateway.port", 1900),
//            openAiApiKey = requireEnv("OPENAI_API_KEY"),
//            openAiModel = System.getenv("OPENAI_MODEL")
//                ?: appConfig.safeString("gateway.providers.openai.model", "llama-3.3-70b-versatile"),
//            openAiBaseUrl = System.getenv("OPENAI_BASE_URL")
//                ?: appConfig.safeString("gateway.providers.openai.baseUrl", "https://api.groq.com/openai/v1"),
            geminiApiKey = requireEnv("GEMINI_API_KEY"),
            geminiModel = System.getenv("GEMINI_MODEL")
                ?: appConfig.safeString("gateway.providers.gemini.model", "gemini-2.5-flash"),
            geminiBaseUrl = System.getenv("GEMINI_BASE_URL")
                ?: appConfig.safeString("gateway.providers.gemini.baseUrl", "https://generativelanguage.googleapis.com/v1beta"),
//            anthropicApiKey = System.getenv("ANTHROPIC_API_KEY"),
//            anthropicModel = System.getenv("ANTHROPIC_MODEL")
//                ?: appConfig.safeString("gateway.providers.anthropic.model", "claude-3-7-sonnet-latest"),
//            anthropicBaseUrl = System.getenv("ANTHROPIC_BASE_URL")
//                ?: appConfig.safeString("gateway.providers.anthropic.baseUrl", "https://api.anthropic.com/v1")
        )
    }

private fun buildJsonConfig(): Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

private fun Application.configureHttp(jsonConfig: Json) {
    install(ContentNegotiation) {
        json(jsonConfig)
    }
}

private fun requireEnv(name: String): String =
    System.getenv(name) ?: throw IllegalStateException("Environment variable '$name' is required")

private fun Config.safeString(path: String, defaultValue: String): String =
    runCatching { getString(path) }.getOrDefault(defaultValue)

private fun Config.safeInt(path: String, defaultValue: Int): Int =
    runCatching { getInt(path) }.getOrDefault(defaultValue)

private fun gatewayOutbounds(config: GatewayConfig): List<OutboundTarget> = buildList {
//    add(
//        OutboundTarget(
//            outboundClass = OpenAiOutboundAdapter::class,
//            model = config.openAiModel,
//            apiKey = config.openAiApiKey,
//            baseUrl = config.openAiBaseUrl
//        )
//    )

    add(
        OutboundTarget(
            outboundClass = GeminiOutboundAdapter::class,
            model = config.geminiModel,
            apiKey = config.geminiApiKey,
            baseUrl = config.geminiBaseUrl
        )
    )

//    add(
//        OutboundTarget(
//            outboundClass = AnthropicOutboundAdapter::class,
//            model = config.anthropicModel,
//            apiKey = config.anthropicApiKey,
//            baseUrl = config.anthropicBaseUrl
//        )
//    )
}


