package org.omniai.gateway.app

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import org.omniai.sdk.core.http.KtorHttpTransportClient
import org.omniai.sdk.gateway.client.assemble
import org.omniai.sdk.gateway.client.dsl.omniAiGateway
import org.omniai.sdk.gateway.client.extensions.anthropic
import org.omniai.sdk.gateway.client.extensions.gemini
import org.omniai.sdk.gateway.client.extensions.openAI
import org.omniai.sdk.gateway.ktor.ClientIpMetadataKey
import org.omniai.sdk.gateway.ktor.openAiConnector
import org.omniai.sdk.gateway.ktor.anthropicConnector
import org.omniai.sdk.gateway.ktor.geminiConnector
import org.omniai.sdk.inbound.openai.OpenAiInboundAdapter
import org.omniai.sdk.inbound.anthropic.AnthropicInboundAdapter
import org.omniai.sdk.inbound.gemini.GeminiInboundAdapter

suspend fun main() {
    val config = loadGatewayConfig()
    val jsonConfig = buildJsonConfig()
    val telemetryRuntime = buildTelemetryRuntime(config)
    val httpClient = KtorHttpTransportClient.default()

    val gateway = omniAiGateway {
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
            when (val auth = config.authConfig) {
                is AuthorizationServerGatewayConfig.None -> none()
                is AuthorizationServerGatewayConfig.Oidc -> discovery {
                    discoveryUrl = auth.discoveryUrl
                    expectedAudience = auth.audience
                    clientId = auth.clientId
                    clientSecret = auth.clientSecret
                }
            }
        }
    }

    val runtime = gateway.assemble(httpClient)

    val server = embeddedServer(Netty, port = config.port) {
        configureHttp(jsonConfig)
        routing {
            openAiConnector(json = jsonConfig).connect(OpenAiInboundAdapter(runtime.service))
            anthropicConnector(json = jsonConfig).connect(AnthropicInboundAdapter(runtime.service))
            geminiConnector(json = jsonConfig).connect(GeminiInboundAdapter(runtime.service))
        }
    }

    server.start(wait = true)
}
