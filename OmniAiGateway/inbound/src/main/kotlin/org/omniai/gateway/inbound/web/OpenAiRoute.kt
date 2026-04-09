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

/**
 * Então o adapter recebe dados reais da request, não um mapa vazio.
 */
private suspend fun handleOpenAiCompletions(
    call: ApplicationCall,
    json: Json,
    adapters: GatewayInboundAdapters
) {
    val request = call.parseBodyOrNull<OpenAiChatCompletionsRequest>(json).respondIfNull(call) ?: return
    val metadata = call.buildRequestMetadataMap(TypedMap())
    callHandler(
        call = call,
        json = json,
        isStream = call.isStreamRequested(request.stream),
        onStream = { adapters.openAi.generateStream(request, metadata) },
        onRest = { adapters.openAi.generate(request, metadata) }
    )
}