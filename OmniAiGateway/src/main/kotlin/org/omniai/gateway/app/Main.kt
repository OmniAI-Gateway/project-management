package org.omniai.gateway.app

import io.ktor.server.engine.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import org.omniai.sdk.core.http.KtorHttpTransportClient
import org.omniai.sdk.gateway.client.dsl.omniAiGateway
import org.omniai.sdk.gateway.client.startServer
import org.omniai.sdk.gateway.client.extensions.outbounds.anthropic
import org.omniai.sdk.gateway.client.extensions.outbounds.gemini
import org.omniai.sdk.gateway.client.extensions.outbounds.openAI
import org.omniai.sdk.gateway.client.extensions.inbounds.openAi
import org.omniai.sdk.gateway.client.extensions.inbounds.anthropic as inboundAnthropic
import org.omniai.sdk.gateway.client.extensions.inbounds.gemini as inboundGemini
import org.omniai.sdk.gateway.ktor.ClientIpMetadataKey
import org.omniai.sdk.gateway.ktor.openAiConnector
import org.omniai.sdk.gateway.ktor.anthropicConnector
import org.omniai.sdk.gateway.ktor.geminiConnector
import org.omniai.sdk.interceptors.auth.AUTH_RESULT_KEY
import org.omniai.sdk.interceptors.auth.domain.AuthValidationResult
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.omniai.sdk.application.pipeline.PipelineResult

suspend fun main() {
    val originalOut = System.out
    val config = loadGatewayConfig()
    val jsonConfig = buildJsonConfig()
    val telemetryRuntime = buildTelemetryRuntime(config)
    val httpClient = KtorHttpTransportClient.default()


    var ktorRoute: Route? = null
    val server = embeddedServer(
        factory = Netty,
        configure = {
            connector {
                port = config.port
                host = "0.0.0.0"
            }
            requestReadTimeoutSeconds = 60
            responseWriteTimeoutSeconds = 120
            tcpKeepAlive = true
        },
        module = {
            configureHttp(jsonConfig)
            routing {
                ktorRoute = this
            }

            buildMcpSetup(originalOut)
        }
    )
    server.start(wait = false)
    val gateway = omniAiGateway {
        inbounds {
            requireNotNull(ktorRoute) { "Ktor routing was not initialized" }.let { route ->
                openAi(route.openAiConnector(json = jsonConfig))
                inboundAnthropic(route.anthropicConnector(json = jsonConfig))
                inboundGemini(route.geminiConnector(json = jsonConfig))
            }
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
                    routing()
                    fallback(metricsPort = if (config.telemetryEnabled) telemetryRuntime.metricsPort else null)
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
                                attribute("aud") { _, _ -> (config.authConfig as?
                                        AuthorizationServerGatewayConfig.Oidc)?.audience ?: "anonymous" }
                            }
                            defaultLatency {
                                enabled = true
                            }
                            activeRequests {
                                enabled = true
                            }
                            ttft {
                                enabled = true
                            }
                            customMetrics {
                                // --- Existing metrics ---
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

                                // --- New Tier 1 metrics ---

                                // 1. Request throughput (total requests)
                                counter(
                                    name = "gateway.requests.total",
                                    description = "Total de requests processados pelo gateway",
                                    unit = "1"
                                ) {
                                    value { _, _ -> 1.0 }
                                    tags { ctx, result ->
                                        val status = when (result) {
                                            is PipelineResult.Unary -> "ok"
                                            is PipelineResult.Stream -> "ok"
                                            is PipelineResult.Error -> "error"
                                            else -> "unknown"
                                        }
                                        mapOf(
                                            "status" to status,
                                            "provider" to ctx.request.provider.value,
                                            "model" to ctx.request.model
                                        )
                                    }
                                }

                                // 2. Input tokens (for cost analysis)
                                counter(
                                    name = "gateway.llm.tokens.input",
                                    description = "Tokens de input consumidos",
                                    unit = "{tokens}"
                                ) {
                                    value { _, result ->
                                        (result as? PipelineResult.Unary)?.response?.usage?.inputTokens?.toDouble()
                                    }
                                }

                                // 3. Output tokens (for cost analysis)
                                counter(
                                    name = "gateway.llm.tokens.output",
                                    description = "Tokens de output gerados",
                                    unit = "{tokens}"
                                ) {
                                    value { _, result ->
                                        (result as? PipelineResult.Unary)?.response?.usage?.outputTokens?.toDouble()
                                    }
                                }

                                // 4. Request size (messages per request)
                                histogram(
                                    name = "gateway.request.size",
                                    description = "Número de mensagens por request",
                                    unit = "{messages}"
                                ) {
                                    value { ctx, _ ->
                                        ctx.request.messages.size.toDouble()
                                    }
                                }

                                // 5. Finish reason tracking
                                counter(
                                    name = "gateway.response.finish_reason",
                                    description = "Contagem por razão de término da resposta",
                                    unit = "1"
                                ) {
                                    value { _, result ->
                                        if (result is PipelineResult.Unary && result.response.choices.isNotEmpty()) 1.0 else null
                                    }
                                    tags { _, result ->
                                        val reason = (result as? PipelineResult.Unary)
                                            ?.response?.choices?.firstOrNull()?.finishReason?.name ?: "unknown"
                                        mapOf("finish_reason" to reason)
                                    }
                                }

                                // 6. Provider errors with detailed breakdown
                                counter(
                                    name = "gateway.provider.errors",
                                    description = "Erros detalhados por provider e tipo de erro",
                                    unit = "1"
                                ) {
                                    value { _, result ->
                                        if (result is PipelineResult.Error) 1.0 else null
                                    }
                                    tags { ctx, result ->
                                        val error = (result as? PipelineResult.Error)?.error
                                        mapOf(
                                            "provider" to ctx.request.provider.value,
                                            "model" to ctx.request.model,
                                            "error.code" to (error?.code?.name ?: "UNKNOWN"),
                                            "error.type" to (error?.let { it::class.simpleName } ?: "Unknown")
                                        )
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

    gateway.startServer(
        httpClient = httpClient,
        onStart = { Thread.currentThread().join() },
        onEnd = { server.stop(1000, 5000) }
    )
}
