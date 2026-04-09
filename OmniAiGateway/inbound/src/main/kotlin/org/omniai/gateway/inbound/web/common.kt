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
import org.omniai.sdk.core.commom.Either
import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.domain.common.AUTH_BEARER_TOKEN_KEY
import org.omniai.sdk.domain.errors.ApiDownError
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.errors.InvalidRequest
import org.omniai.sdk.domain.errors.ParsingError
import org.omniai.sdk.domain.errors.ProviderApiError
import org.omniai.sdk.inbound.anthropic.AnthropicInboundAdapter
import org.omniai.sdk.inbound.gemini.GeminiInboundAdapter
import org.omniai.sdk.inbound.openai.OpenAiInboundAdapter

data class GatewayInboundAdapters(
    val anthropic: AnthropicInboundAdapter,
    val openAi: OpenAiInboundAdapter,
    val gemini: GeminiInboundAdapter
)
/**
* tira o token do header HTTP e passá-lo para o pipeline interno.
 */
fun ApplicationCall.extractBearerTokenOrNull(): String? {
    val header = request.headers[HttpHeaders.Authorization]?.trim().orEmpty()
    if (!header.startsWith("Bearer ", ignoreCase = true)) {
        return null
    }
    return header.substringAfter(' ', "").trim().takeIf { it.isNotBlank() }
}

/**
 * Cria/usa um TypedMap de metadata da request.
 * Chama extractBearerTokenOrNull().
 * Se encontrou token, guarda no mapa com chave AUTH_BEARER_TOKEN_KEY.
 */
fun ApplicationCall.buildRequestMetadataMap(base: TypedMap = TypedMap()): TypedMap = base.also {
    extractBearerTokenOrNull()?.let { bearer ->
        it.put(AUTH_BEARER_TOKEN_KEY, bearer)
    }
}

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

suspend fun respondDomainError(call: ApplicationCall, error: DomainError) {
    val status = when (error) {
        is InvalidRequest, is ParsingError -> HttpStatusCode.BadRequest
        is ApiDownError -> HttpStatusCode.ServiceUnavailable
        is ProviderApiError -> HttpStatusCode.BadGateway
        else -> HttpStatusCode.InternalServerError
    }
    call.respond(status, mapOf("error" to error.message))
}

suspend inline fun <reified T : Any> callHandler(
    call: ApplicationCall,
    json: Json,
    isStream: Boolean,
    crossinline onStream: suspend () -> Either<DomainError, Flow<T>>,
    crossinline onRest: suspend () -> Either<DomainError, T>
) {
    runCatching {
        if (isStream) {
            when (val streamResult = onStream()) {
                is Either.Left -> respondDomainError(call, streamResult.value)
                is Either.Right -> {
                    call.response.headers.append(HttpHeaders.CacheControl, "no-cache")
                    call.response.headers.append(HttpHeaders.Connection, "keep-alive")
                    call.respondTextWriter(
                        contentType = ContentType.Text.EventStream,
                        status = HttpStatusCode.OK
                    ) {
                        streamResult.value.collect { event ->
                            val eventJson = json.encodeToString(event)
                            write("data: $eventJson\n\n")
                            flush()
                        }
                    }
                }
            }
        } else {
            when (val restResult = onRest()) {
                is Either.Left -> respondDomainError(call, restResult.value)
                is Either.Right -> call.respond<T>(HttpStatusCode.OK, restResult.value)
            }
        }
    }.onFailure { respondUnexpectedError(call, it) }
}