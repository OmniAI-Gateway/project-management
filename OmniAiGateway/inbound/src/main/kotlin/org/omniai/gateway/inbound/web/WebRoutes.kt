package org.omniai.gateway.inbound.web

import io.ktor.server.application.Application
import kotlinx.serialization.json.Json

fun Application.installWebRoutes(
    json: Json,
    adapters: GatewayInboundAdapters
) {
    installAnthropicRoute(json, adapters)
    installOpenAiRoute(json, adapters)
    installGeminiRoute(json, adapters)
}
