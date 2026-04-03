package org.omniai.gateway.inbound.web

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.omniai.sdk.contracts.openai.input.OpenAiChatCompletionsRequest
import org.omniai.sdk.core.commom.TypedMap

fun Application.installOpenAiRoute(
    json: Json,
    adapters: GatewayInboundAdapters
) {
    routing {
        post("/v1/chat/completions") {
            handleOpenAiCompletions(call, json, adapters)
        }
    }
}

private suspend fun handleOpenAiCompletions(
    call: ApplicationCall,
    json: Json,
    adapters: GatewayInboundAdapters
) {
    val request = call.parseBodyOrNull<OpenAiChatCompletionsRequest>(json).respondIfNull(call) ?: return
    callHandler(
        call = call,
        json = json,
        isStream = call.isStreamRequested(request.stream),
        onStream = { adapters.openAi.generateStream(request, TypedMap()) },
        onRest = { adapters.openAi.generate(request, TypedMap()) }
    )
}