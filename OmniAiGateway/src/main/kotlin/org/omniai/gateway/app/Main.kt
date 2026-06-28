package org.omniai.gateway.app

import io.ktor.server.websocket.WebSockets
import io.ktor.server.application.install
import io.ktor.server.engine.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.cio.CIO
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope.coroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import org.omniai.sdk.core.http.KtorHttpTransportClient
import org.omniai.sdk.gateway.client.dsl.omniAiGateway
import org.omniai.sdk.gateway.client.startServer


suspend fun main() {
    val config = loadGatewayConfig()
    val jsonConfig = buildJsonConfig()
    val telemetryRuntime = buildTelemetryRuntime(config)
    val httpClient = KtorHttpTransportClient.default()
    var ktorRoute: Route? = null
    val server = embeddedServer(
        factory = CIO,
        configure = {
            connector {
                port = config.port
                host = "0.0.0.0"
            }
            connectionIdleTimeoutSeconds = 60
        },
        module = {
            launch {
                install(SSE)
                install(WebSockets)
                configureHttp(jsonConfig)
                configureGatewayCors()
                routing {
                    ktorRoute = this
                }
                buildMcpSetup()
            }
        }
    )
    server.start(wait = false)

    val gateway = omniAiGateway {
        inbounds {
            registerStandardInbounds(requireNotNull(ktorRoute) { "Ktor routing was not initialized" }, jsonConfig)
        }
        execution {
            useNativePipeline {
                outbounds {
                    registerProvidersFromConfig(config.providers, httpClient)
                }

                interceptors {
                    routing()
                    fallback(metricsPort = if (config.telemetryEnabled) telemetryRuntime.metricsPort else null)
                    registerGatewayMetrics(config, telemetryRuntime)
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
        onStart = { println("[Gateway] Server successfully started.") },
//        onEnd = { server.stop(1000, 5000) }
    )

    try {
        awaitCancellation()
    } catch (e: CancellationException) {
        println("========== MAIN CANCELLED ==========")
        e.printStackTrace()

        println(coroutineContext[Job])

        throw e
    }
}
