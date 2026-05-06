package org.omniai.gateway.app

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import org.omniai.sdk.core.http.KtorHttpTransportClient
import org.omniai.sdk.core.pipeline.PipelineResult
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
import org.omniai.sdk.interceptors.auth.AUTH_RESULT_KEY
import org.omniai.sdk.interceptors.auth.domain.AuthValidationResult
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

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
                        metrics {
                            metricsPort = telemetryRuntime.metricsPort
                            tracer = telemetryRuntime.tracer
                            attributes {
                                attribute("user.email") { gContext, _ ->
                                    val authResult = gContext.attributes[AUTH_RESULT_KEY]
                                    (authResult as? AuthValidationResult.Jwt)
                                        ?.decoded?.payload?.privateClaims?.get("email")?.jsonPrimitive?.contentOrNull
                                }
                                attribute("user.username") { gContext, _ ->
                                    val authResult = gContext.attributes[AUTH_RESULT_KEY]
                                    (authResult as? AuthValidationResult.Jwt)
                                        ?.decoded?.payload?.privateClaims
                                        ?.get("preferred_username")
                                        ?.jsonPrimitive?.contentOrNull
                                        ?: (authResult as? AuthValidationResult.Opaque)?.introspectionResult?.username
                                }
                                attribute("user.sub") { gContext, _ ->
                                    when (val authResult = gContext.attributes[AUTH_RESULT_KEY]) {
                                        is AuthValidationResult.Jwt -> authResult.decoded.payload.subject
                                        is AuthValidationResult.Opaque -> authResult.introspectionResult.sub
                                        else -> null
                                    }
                                }
                                include(ClientIpMetadataKey, alias = "client.ip")
                                attribute("discovery") { _, _ -> (config.authConfig as?
                                        AuthorizationServerGatewayConfig.Oidc)?.discoveryUrl ?: "discovery" }
                                attribute("sdk.version") { _, _ -> "1.0.0" }
                                attribute("aud") { _, _ -> (config.authConfig as?  AuthorizationServerGatewayConfig.Oidc)?.audience ?: "anonymous" }
                            }
                            defaultLatency {
                                enabled = true
                            }
                            customMetrics {
                                
                                counter(
                                    name = "gateway.llm.tokens",
                                    description = "Total de tokens consumidos",
                                    unit = "{tokens}"
                                ) {
                                    value { _, result ->
                                        (result as? PipelineResult.Unary)?.response?.usage?.totalTokens
                                    }
                                }
                                counter(
                                    name = "gateway.requests.errors",
                                    description = "Número de requests falhados",
                                    unit = "1"
                                ) {
                                    value { _, result ->
                                        if (result is PipelineResult.Error) 1.0 else null
                                    }
                                    tags { _, result ->
                                        val error = result as? PipelineResult.Error
                                        mapOf("error.message" to (error?.error?.message ?: "unknown"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        security {
            authentication {
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
            authorization { }
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
