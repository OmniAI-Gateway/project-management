package org.omniai.gateway.app

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import org.omniai.sdk.core.http.KtorHttpTransportClient
import org.omniai.sdk.gateway.client.assemble
import org.omniai.sdk.gateway.client.gatewayConfig
import org.omniai.sdk.gateway.client.outbound.buildOutbounds
import org.omniai.sdk.gateway.ktor.ClientIpMetadataKey
import org.omniai.sdk.gateway.ktor.installAiGateway

suspend fun main() {
    val config = loadGatewayConfig()
    val jsonConfig = buildJsonConfig()
    val outbounds = buildOutbounds(gatewayOutbounds(config))
    val telemetryRuntime = buildTelemetryRuntime(config)

    val definition = gatewayConfig {
        outbounds {
            outbounds.forEach { outbound -> +outbound }
        }

        metrics {
            enabled = config.telemetryEnabled
            if (enabled) {
                telemetry {
                    tags(ClientIpMetadataKey)
                    meter = telemetryRuntime.meter
                    telemetryRuntime.tracer?.let { tracer = it }
                }
            }
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



