package org.omniai.gateway.inbound.web

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.core.commom.TypedMap

fun Application.installAnthropicRoute(
    json: Json,
    adapters: GatewayInboundAdapters
) {
    routing {
        post("/v1/messages") {
            handleAnthropicMessages(call, json, adapters)
        }
    }
}

private suspend fun handleAnthropicMessages(
    call: ApplicationCall,
    json: Json,
    adapters: GatewayInboundAdapters
) {
    val request = call.parseBodyOrNull<AnthropicMessagesRequest>(json).respondIfNull(call) ?: return
    callHandler(
        call = call,
        json = json,
        isStream = call.isStreamRequested(request.stream),
        onStream = { adapters.anthropic.generateStream(request, TypedMap()) },
        onRest = { adapters.anthropic.generate(request, TypedMap()) }
    )
}
