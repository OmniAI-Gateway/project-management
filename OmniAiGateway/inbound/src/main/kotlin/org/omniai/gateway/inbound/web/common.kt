package org.omniai.gateway.inbound.web

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondTextWriter
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import org.omniai.sdk.inbound.anthropic.AnthropicInboundAdapter
import org.omniai.sdk.inbound.gemini.GeminiInboundAdapter
import org.omniai.sdk.inbound.openai.OpenAiInboundAdapter

data class GatewayInboundAdapters(
	val anthropic: AnthropicInboundAdapter,
	val openAi: OpenAiInboundAdapter,
	val gemini: GeminiInboundAdapter
)

suspend inline fun <reified T> ApplicationCall.parseBodyOrNull(json: Json): T? {
    val rawBody = runCatching { receiveText() }.getOrNull() ?: return null
    return runCatching { json.decodeFromString<T>(rawBody) }.getOrNull()
}

suspend fun <T> T?.respondIfNull(call: ApplicationCall): T? {
    if (this == null) {
        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request body"))
    }
    return this
}

suspend fun respondUnexpectedError(call: ApplicationCall, throwable: Throwable) {
	println("[gateway] request failed: ${throwable.message}")
	call.respond(
		HttpStatusCode.InternalServerError,
		mapOf("error" to "Internal server error")
	)
}

suspend inline fun <reified T : Any> callHandler(
    call: ApplicationCall,
    json: Json,
    isStream: Boolean,
    crossinline onStream: suspend () -> Flow<T>,
    crossinline onRest: suspend () -> T
) {
    runCatching {
        if (isStream) {
            call.response.headers.append(HttpHeaders.CacheControl, "no-cache")
            call.response.headers.append(HttpHeaders.Connection, "keep-alive")
            call.respondTextWriter(
                contentType = ContentType.Text.EventStream,
                status = HttpStatusCode.OK
            ) {
                onStream().collect { event ->
                    val eventJson = json.encodeToString(event)
                    write("data: $eventJson\n\n")
                    flush()
                }
            }
        } else {
            call.respond<T>(HttpStatusCode.OK, onRest())
        }
    }.onFailure { respondUnexpectedError(call, it) }
}