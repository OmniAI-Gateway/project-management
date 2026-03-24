package org.omniaigateway.app

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.omniaigateway.adapters.openai.OpenAiOutboundAdapter
import org.omniaigateway.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniaigateway.domain.common.Model
import org.omniaigateway.inbound.anthropic.AnthropicInboundAdapter

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
    println("key: $openAiApiKey")
    val service = OpenAiDelegatingInferenceService(openAiOutbound)
    val anthropicInboundAdapter = AnthropicInboundAdapter(service)

    embeddedServer(Netty, port = port) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    encodeDefaults = false
                }
            )
        }

        routing {
            post("/v1/messages") {
                val request = call.receive<AnthropicMessagesRequest>()

                if (request.stream == true) {
                    call.respondText(
                        status = HttpStatusCode.NotImplemented,
                        text = "stream=true is not implemented yet for the OpenAI outbound adapter"
                    )
                    return@post
                }

                val response = anthropicInboundAdapter.generate(request)
                println("RESPONSE:" + Json.encodeToString(response))
                call.respond(HttpStatusCode.OK, response)
            }
        }
    }.start(wait = true)
}

private fun requireEnv(name: String): String =
    System.getenv(name) ?: throw IllegalStateException("Environment variable '$name' is required")
