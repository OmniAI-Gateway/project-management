package org.omniai.gateway.app

import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveText
import io.ktor.server.response.cacheControl
import io.ktor.server.response.respond
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.SerializationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import org.omniai.sdk.adapters.gemini.GeminiOutboundAdapter
import org.omniai.sdk.adapters.openai.OpenAiOutboundAdapter
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.output.AnthropicMessageResponse
import org.omniai.sdk.contracts.anthropic.output.AnthropicOutputContent
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamDelta
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent
import org.omniai.sdk.contracts.anthropic.output.MessageDeltaInfo
import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.core.pipeline.MetricsInterceptor
import org.omniai.sdk.core.pipeline.ProviderModelMetrics
import org.omniai.sdk.core.pipeline.gatewayPipeline
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.inbound.anthropic.AnthropicInboundAdapter
import kotlin.time.Duration.Companion.seconds

private val providerSemaphore = Semaphore(1)

private data class GatewayConfig(
    val port: Int,
    val openAiApiKey: String,
    val openAiModel: String,
    val openAiBaseUrl: String,
    val geminiApiKey: String,
    val geminiModel: String,
    val geminiBaseUrl: String
)

suspend fun <T> executeWithProviderCooldown(block: suspend () -> T): T {
    return providerSemaphore.withPermit {
        val result = block()
        delay(5.seconds)
        result
    }
}

fun main() {
    val config = loadGatewayConfig()
    val jsonConfig = buildJsonConfig()
    val service = buildInferenceService(config)
    val anthropicInboundAdapter = AnthropicInboundAdapter(service)

    startServer(
        port = config.port,
        jsonConfig = jsonConfig,
        anthropicInboundAdapter = anthropicInboundAdapter
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
        geminiBaseUrl = System.getenv("GEMINI_BASE_URL") ?: "https://generativelanguage.googleapis.com/v1beta/models/{model}"
    )

private fun buildJsonConfig(): Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

private fun buildInferenceService(config: GatewayConfig): InferenceServicePort {
    val openAiOutbound = OpenAiOutboundAdapter(
        model = Model(config.openAiModel),
        apiKey = config.openAiApiKey,
        baseUrl = config.openAiBaseUrl
    )

    val geminiOutbound = GeminiOutboundAdapter(
        model = Model(config.geminiModel),
        apiKey = config.geminiApiKey,
        baseUrl = config.geminiBaseUrl
    )

    val rawService = OpenAiGeminiFallbackInferenceService(
        openAiOutboundAdapter = openAiOutbound,
        geminiOutboundAdapter = geminiOutbound,
        geminiFallbackModel = config.geminiModel
    )

    val pipeline = gatewayPipeline {
        install(MetricsInterceptor())
        installService(rawService)
    }

    return PipelineBackedInferenceService(
        pipeline = pipeline,
        onMetricsCaptured = { _, metrics: ProviderModelMetrics ->
            println(
                "[metrics] provider=${metrics.provider} model=${metrics.model} " +
                    "requests=${metrics.totalRequests} success=${metrics.successCount} errors=${metrics.errorCount} " +
                    "successRate=${metrics.successRate} avgLatencyMs=${metrics.averageLatencyMs} totalTokens=${metrics.totalTokens}"
            )
        }
    )
}

private fun startServer(
    port: Int,
    jsonConfig: Json,
    anthropicInboundAdapter: AnthropicInboundAdapter
) {
    embeddedServer(Netty, port = port) {
        configureHttp(jsonConfig)
        configureRoutes(jsonConfig, anthropicInboundAdapter)
    }.start(wait = true)
}

private fun Application.configureHttp(jsonConfig: Json) {
    install(ContentNegotiation) {
        json(jsonConfig)
    }
}

private fun Application.configureRoutes(
    jsonConfig: Json,
    anthropicInboundAdapter: AnthropicInboundAdapter
) {
    routing {
        post("/v1/messages") {
            handleMessagesRequest(call, jsonConfig, anthropicInboundAdapter)
        }
    }
}

private suspend fun handleMessagesRequest(
    call: ApplicationCall,
    jsonConfig: Json,
    anthropicInboundAdapter: AnthropicInboundAdapter
) {
    val rawBody = call.receiveText()
    val request = parseAnthropicRequest(call, jsonConfig, rawBody) ?: return

    try {
        val response = executeWithProviderCooldown {
            anthropicInboundAdapter.generate(request, TypedMap())
        }

        if (request.stream == true) {
            respondAsServerSentEvents(call, jsonConfig, response)
            return
        }

        call.respond(HttpStatusCode.OK, response)
    } catch (e: Exception) {
        println("Error processing /v1/messages: ${e.message}")
        call.respond(
            HttpStatusCode.InternalServerError,
            mapOf("error" to "Internal server error")
        )
    }
}

private suspend fun parseAnthropicRequest(
    call: ApplicationCall,
    jsonConfig: Json,
    rawBody: String
): AnthropicMessagesRequest? {
    return try {
        jsonConfig.decodeFromString<AnthropicMessagesRequest>(rawBody)
    } catch (e: SerializationException) {
        println("JSON parse error on /v1/messages: ${e.message}")
        println("Raw body: $rawBody")
        call.respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to "Invalid request body", "details" to (e.message ?: "Serialization error"))
        )
        null
    } catch (e: Exception) {
        println("Unexpected parse error on /v1/messages: ${e.message}")
        e.printStackTrace()
        call.respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to "Invalid request body")
        )
        null
    }
}

private suspend fun respondAsServerSentEvents(
    call: ApplicationCall,
    jsonConfig: Json,
    fullResponse: AnthropicMessageResponse
) {
    val generatedText = fullResponse.content
        .filterIsInstance<AnthropicOutputContent.Text>()
        .joinToString(separator = "") { it.text }

    call.response.cacheControl(CacheControl.NoCache(null))
    call.respondTextWriter(contentType = ContentType.Text.EventStream) {
        fun emitEvent(event: AnthropicStreamEvent) {
            write("event: ${event.type}\n")
            write("data: ${jsonConfig.encodeToString(event)}\n\n")
            flush()
        }

        emitEvent(
            AnthropicStreamEvent.MessageStart(
                message = fullResponse.copy(content = emptyList())
            )
        )
        emitEvent(
            AnthropicStreamEvent.ContentBlockStart(
                index = 0,
                contentBlock = AnthropicOutputContent.Text(text = "")
            )
        )

        for (chunk in generatedText.chunked(5)) {
            emitEvent(
                AnthropicStreamEvent.ContentBlockDelta(
                    index = 0,
                    delta = AnthropicStreamDelta.TextDelta(text = chunk)
                )
            )
            delay(20)
        }

        emitEvent(AnthropicStreamEvent.ContentBlockStop(index = 0))
        emitEvent(
            AnthropicStreamEvent.MessageDelta(
                delta = MessageDeltaInfo(
                    stopReason = fullResponse.stopReason ?: "end_turn",
                    stopSequence = fullResponse.stopSequence
                ),
                usage = fullResponse.usage
            )
        )
        emitEvent(AnthropicStreamEvent.MessageStop())
    }
}

private fun requireEnv(name: String): String =
    System.getenv(name) ?: throw IllegalStateException("Environment variable '$name' is required")
