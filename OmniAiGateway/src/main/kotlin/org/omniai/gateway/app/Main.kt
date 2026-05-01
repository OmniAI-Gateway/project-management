package org.omniai.gateway.app

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import org.omniai.sdk.core.http.KtorHttpTransportClient
import org.omniai.sdk.gateway.client.dsl.omniAiGateway
import org.omniai.sdk.gateway.client.startServer
import org.omniai.sdk.gateway.ktor.ClientIpMetadataKey
import org.omniai.sdk.gateway.ktor.openAiConnector
import org.omniai.sdk.gateway.ktor.anthropicConnector
import org.omniai.sdk.gateway.ktor.geminiConnector
import org.omniai.sdk.core.ports.InboundConnector
import org.omniai.sdk.gateway.client.extensions.openAI
import org.omniai.sdk.gateway.client.extensions.gemini
import org.omniai.sdk.gateway.client.extensions.anthropic
import org.omniai.sdk.contracts.openai.input.OpenAiChatCompletionsRequest
import org.omniai.sdk.contracts.openai.output.OpenAiChatCompletionsResponse
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.output.AnthropicMessageResponse
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent
import org.omniai.sdk.contracts.gemini.input.GeminiGenerateContentRequest
import org.omniai.sdk.contracts.gemini.output.GeminiGenerateContentResponse

suspend fun main() {
    val config = loadGatewayConfig()
    val jsonConfig = buildJsonConfig()
    val telemetryRuntime = buildTelemetryRuntime(config)
    val httpClient = KtorHttpTransportClient.default()

    var openAiConn: InboundConnector<OpenAiChatCompletionsRequest, OpenAiChatCompletionsResponse, OpenAiChatCompletionsResponse>? = null
    var anthropicConn: InboundConnector<AnthropicMessagesRequest, AnthropicMessageResponse, AnthropicStreamEvent>? = null
    var geminiConn: InboundConnector<GeminiGenerateContentRequest, GeminiGenerateContentResponse, GeminiGenerateContentResponse>? = null

    val server = embeddedServer(Netty, port = config.port) {
        configureHttp(jsonConfig)
        routing {
            if (config.providers.any { it.provider == ProviderKind.OPENAI }) {
                openAiConn = openAiConnector(json = jsonConfig)
            }
            if (config.providers.any { it.provider == ProviderKind.ANTHROPIC }) {
                anthropicConn = anthropicConnector(json = jsonConfig)
            }
            if (config.providers.any { it.provider == ProviderKind.GEMINI }) {
                geminiConn = geminiConnector(json = jsonConfig)
            }
        }
    }

    val gateway = omniAiGateway {
        inbounds {
            openAiConn?.let { openAi(it) }
            anthropicConn?.let { anthropic(it) }
            geminiConn?.let { gemini(it) }
        }

        execution {
            useNativePipeline {
                outbounds {
                    config.providers.forEach { providerConfig ->
                        when (providerConfig.provider) {
                            ProviderKind.OPENAI -> openAI(httpClient) {
                                baseUrl(providerConfig.baseUrl)
                                apiKey(providerConfig.apiKey) {
                                    models(*providerConfig.models.toTypedArray())
                                }
                            }
                            ProviderKind.GEMINI -> gemini(httpClient) {
                                baseUrl(providerConfig.baseUrl)
                                apiKey(providerConfig.apiKey) {
                                    models(*providerConfig.models.toTypedArray())
                                }
                            }
                            ProviderKind.ANTHROPIC -> anthropic(httpClient) {
                                baseUrl(providerConfig.baseUrl)
                                apiKey(providerConfig.apiKey) {
                                    models(*providerConfig.models.toTypedArray())
                                }
                            }
                        }
                    }
                }

                interceptors {
                    if (config.telemetryEnabled) {
                        telemetryMetrics {
                            meter = telemetryRuntime.meter
                            tracer = telemetryRuntime.tracer
                            attributes {
                                include(ClientIpMetadataKey)
                            }
                        }
                    }
                }
            }
        }

        authorizationServer {
            none()
        }
    }

    gateway.startServer(httpClient) {
        server.start(wait = true)
    }
}
