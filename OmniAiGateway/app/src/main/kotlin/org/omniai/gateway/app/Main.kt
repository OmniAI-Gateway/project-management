package org.omniai.gateway.app

import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import org.omniai.sdk.adapters.openai.OpenAiOutboundAdapter
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.output.AnthropicOutputContent
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamDelta
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent
import org.omniai.sdk.contracts.anthropic.output.MessageDeltaInfo
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.inbound.anthropic.AnthropicInboundAdapter
import kotlinx.serialization.SerializationException
import kotlin.time.Duration.Companion.seconds

val groqSemaphore = Semaphore(1)

suspend fun <T> generate(f :suspend ()-> T): T{
    return groqSemaphore.withPermit {
        val f = f()
        delay(5.seconds)
        return f
    }
}


fun main() {
    val openAiApiKey = requireEnv("OPENAI_API_KEY")
    val openAiModel = System.getenv("OPENAI_MODEL") ?: "llama-3.3-70b-versatile"
    val openAiBaseUrl = System.getenv("OPENAI_BASE_URL") ?: "https://api.groq.com/openai/v1"

    val port = (System.getenv("PORT") ?: "1900").toInt()

    val openAiOutbound = OpenAiOutboundAdapter(
        model = Model(openAiModel),
        apiKey = openAiApiKey,
        baseUrl = openAiBaseUrl
    )
    val service = OpenAiDelegatingInferenceService(openAiOutbound)
    val anthropicInboundAdapter = AnthropicInboundAdapter(service)

    val jsonConfig = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

    embeddedServer(Netty, port = port) {
        install(ContentNegotiation) {
            json(jsonConfig)
        }

        routing {
            post("/v1/messages") {
                val rawBody = call.receiveText()
                val request = try {
                    jsonConfig.decodeFromString<AnthropicMessagesRequest>(rawBody)
                } catch (e: SerializationException) {
                    println("JSON parse error on /v1/messages: ${e.message}")
                    println("Raw body: $rawBody")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid request body", "details" to (e.message ?: "Serialization error"))
                    )
                    return@post
                } catch (e: Exception) {
                    println("Unexpected parse error on /v1/messages: ${e.message}")
                    e.printStackTrace()
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid request body")
                    )
                    return@post
                }

                try {
                    if (request.stream == true) {
                        val fullResponse =  generate {
                            anthropicInboundAdapter.generate(request)
                        }
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
                        return@post
                    }

                    val response = generate {
                        anthropicInboundAdapter.generate(request)
                    }
                    call.respond(HttpStatusCode.OK, response)

                } catch (e: Exception) {
                    println("Error processing /v1/messages: ${e.message}")
                    e.printStackTrace()
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Internal server error")
                    )
                }
            }
        }
    }.start(wait = true)
}

private fun requireEnv(name: String): String =
    System.getenv(name) ?: throw IllegalStateException("Environment variable '$name' is required")
