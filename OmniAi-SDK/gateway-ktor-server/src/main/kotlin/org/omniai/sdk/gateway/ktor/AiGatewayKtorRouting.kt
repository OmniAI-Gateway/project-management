package org.omniai.sdk.gateway.ktor

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import org.omniai.sdk.binders.ConfigurableMetadataBinder
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.output.AnthropicMessageResponse
import org.omniai.sdk.contracts.gemini.input.GeminiGenerateContentRequest
import org.omniai.sdk.contracts.gemini.output.GeminiGenerateContentResponse
import org.omniai.sdk.contracts.openai.input.OpenAiChatCompletionsRequest
import org.omniai.sdk.contracts.openai.output.OpenAiChatCompletionsResponse
import org.omniai.sdk.common.Either
import org.omniai.sdk.common.TypedMap
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.ports.inbound.InboundConnector
import org.omniai.sdk.core.http.ErrorHttpMapper

val defaultGatewayJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

fun Route.openAiConnector(
    path: String = "/v1/chat/completions",
    json: Json = defaultGatewayJson,
    metadataBinder: ConfigurableMetadataBinder = defaultKtorRequestMetadataBinder()
): InboundConnector<OpenAiChatCompletionsRequest, OpenAiChatCompletionsResponse, OpenAiChatCompletionsResponse> {
    return InboundConnector { port ->
        post(path) {
            val request = call.parseBodyOrNull<OpenAiChatCompletionsRequest>(json).respondIfNull(call) ?: return@post
            val metadata = call.bindRequestMetadata(metadataBinder)
            call.handleGatewayResponse(
                json = json,
                stream = call.isStreamRequested(request.stream),
                onStream = { port.generateStream(request, metadata) },
                onUnary = { port.generate(request, metadata) }
            )
        }
    }
}

fun Route.anthropicConnector(
    path: String = "/v1/messages",
    json: Json = defaultGatewayJson,
    metadataBinder: ConfigurableMetadataBinder = defaultKtorRequestMetadataBinder()
): InboundConnector<AnthropicMessagesRequest, AnthropicMessageResponse, org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent> {
    return InboundConnector { port ->
        post(path) {
            val request = call.parseBodyOrNull<AnthropicMessagesRequest>(json).respondIfNull(call) ?: return@post
            val metadata = call.bindRequestMetadata(metadataBinder)
            call.handleGatewayResponse(
                json = json,
                stream = call.isStreamRequested(request.stream),
                onStream = { port.generateStream(request, metadata) },
                onUnary = { port.generate(request, metadata) }
            )
        }
    }
}

fun Route.geminiConnector(
    path: String = "/v1beta/models/{model}:generateContent",
    json: Json = defaultGatewayJson,
    metadataBinder: ConfigurableMetadataBinder = defaultKtorRequestMetadataBinder()
): InboundConnector<GeminiGenerateContentRequest, GeminiGenerateContentResponse, GeminiGenerateContentResponse> {
    return InboundConnector { port ->
        post(path) {
            val request = call.parseBodyOrNull<GeminiGenerateContentRequest>(json).respondIfNull(call) ?: return@post
            val metadata = call.bindRequestMetadata(metadataBinder)
            call.handleGatewayResponse(
                json = json,
                stream = call.isStreamRequested(explicitFlag = null),
                onStream = { port.generateStream(request, metadata) },
                onUnary = { port.generate(request, metadata) }
            )
        }
    }
}

private fun ApplicationCall.bindRequestMetadata(binder: ConfigurableMetadataBinder): TypedMap {
    val clientIp = extractClientIp()
    val context = KtorIncomingContext(this, clientIp)
    return binder.bind(context).also { typedMap ->
        if (!clientIp.isNullOrBlank() && !typedMap.contains(ClientIpMetadataKey)) {
            typedMap.put(CLIENT_IP_METADATA_KEY, clientIp)
        }
    }
}

private suspend inline fun <reified T> ApplicationCall.parseBodyOrNull(json: Json): T? {
    val rawBody = runCatching { receiveText() }.getOrNull() ?: return null
    return runCatching { json.decodeFromString<T>(rawBody) }.getOrNull()
}

private suspend fun <T> T?.respondIfNull(call: ApplicationCall): T? {
    if (this == null) {
        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request body"))
    }
    return this
}

private suspend inline fun <reified UnaryT : Any, reified StreamT : Any> ApplicationCall.handleGatewayResponse(
    json: Json,
    stream: Boolean,
    crossinline onStream: suspend () -> Either<DomainError, Flow<StreamT>>,
    crossinline onUnary: suspend () -> Either<DomainError, UnaryT>
) {
    runCatching {
        if (stream) {
            when (val result = onStream()) {
                is Either.Left -> respondDomainError(result.value)
                is Either.Right -> respondAsSse(result.value, json)
            }
        } else {
            when (val result = onUnary()) {
                is Either.Left -> respondDomainError(result.value)
                is Either.Right -> respond(HttpStatusCode.OK, result.value)
            }
        }
    }.onFailure { throwable ->
        respond(
            HttpStatusCode.InternalServerError,
            mapOf("error" to (throwable.message ?: "Internal server error"))
        )
    }
}

private suspend inline fun <reified T : Any> ApplicationCall.respondAsSse(eventFlow: Flow<T>, json: Json) {
    response.headers.append(HttpHeaders.CacheControl, "no-cache")
    response.headers.append(HttpHeaders.Connection, "keep-alive")

    respondTextWriter(
        contentType = ContentType.Text.EventStream,
        status = HttpStatusCode.OK
    ) {
        eventFlow.collect { event ->
            val payload = json.encodeToString(event)
            write("data: $payload\n\n")
            flush()
        }
    }
}

private suspend fun ApplicationCall.respondDomainError(error: DomainError) {
    val status = ErrorHttpMapper.toHttpStatusCode(error.code)
    respond(status, mapOf("error" to error.message))
}

private fun ApplicationCall.isStreamRequested(explicitFlag: Boolean?): Boolean =
    explicitFlag ?: request.queryParameters["stream"]?.toBooleanStrictOrNull() ?: false
