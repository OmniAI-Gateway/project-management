package org.omniai.gateway.inbound.web

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.omniai.sdk.contracts.gemini.input.GeminiGenerateContentRequest
import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.inbound.gemini.GeminiInboundAdapter.Companion.GEMINI_MODEL_KEY

fun Application.installGeminiRoute(
    json: Json,
    adapters: GatewayInboundAdapters
) {
    routing {
        post("/v1beta/models/{model}:generateContent") {
            handleGeminiGenerateContent(call, json, adapters)
        }
    }
}

private suspend fun handleGeminiGenerateContent(
    call: ApplicationCall,
    json: Json,
    adapters: GatewayInboundAdapters
) {
    val request = call.parseBodyOrNull<GeminiGenerateContentRequest>(json).respondIfNull(call) ?: return
    val model = call.parameters["model"]
    val map = call.buildRequestMetadataMap(TypedMap()).also {
        if (!model.isNullOrBlank()) {
            it.put(GEMINI_MODEL_KEY, model)
        }
    }
    callHandler(
        call = call,
        json = json,
        isStream = call.isStreamRequested(explicitFlag = null),
        onStream = { adapters.gemini.generateStream(request, map) },
        onRest = { adapters.gemini.generate(request, map) }
    )
}

fun ApplicationCall.isStreamRequested(explicitFlag: Boolean?): Boolean =
    explicitFlag
        ?: request.queryParameters["stream"]?.toBooleanStrictOrNull()
        ?: false
