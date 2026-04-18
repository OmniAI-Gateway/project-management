package org.omniai.sdk.gateway.ktor

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import org.omniai.sdk.binders.ConfigurableMetadataBinder
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.gemini.input.GeminiGenerateContentRequest
import org.omniai.sdk.contracts.openai.input.OpenAiChatCompletionsRequest
import org.omniai.sdk.core.commom.Either
import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.domain.errors.ApiDownError
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.errors.InvalidRequest
import org.omniai.sdk.domain.errors.ParsingError
import org.omniai.sdk.domain.errors.ProviderApiError
import org.omniai.sdk.gateway.client.GatewayNetworkAdapter
import org.omniai.sdk.gateway.client.GatewayRuntime

class AiGatewayKtorConfigDsl {
    var pathPrefix: String = ""
    var openAiPath: String = "/v1/chat/completions"
    var anthropicPath: String = "/v1/messages"
    var geminiPath: String = "/v1beta/models/{model}:generateContent"
    var installOpenAi: Boolean = true
    var installAnthropic: Boolean = true
    var installGemini: Boolean = true
    var json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
    var metadataBinder: ConfigurableMetadataBinder = defaultKtorRequestMetadataBinder()

    internal fun build(): AiGatewayKtorConfig = AiGatewayKtorConfig(
        pathPrefix = normalizePrefix(pathPrefix),
        openAiPath = openAiPath,
        anthropicPath = anthropicPath,
        geminiPath = geminiPath,
        installOpenAi = installOpenAi,
        installAnthropic = installAnthropic,
        installGemini = installGemini,
        json = json,
        metadataBinder = metadataBinder
    )
}

data class AiGatewayKtorConfig(
    val pathPrefix: String,
    val openAiPath: String,
    val anthropicPath: String,
    val geminiPath: String,
    val installOpenAi: Boolean,
    val installAnthropic: Boolean,
    val installGemini: Boolean,
    val json: Json,
    val metadataBinder: ConfigurableMetadataBinder
)

fun Routing.installAiGateway(
    runtime: GatewayRuntime,
    configure: AiGatewayKtorConfigDsl.() -> Unit = {}
) {
    val config = AiGatewayKtorConfigDsl().apply(configure).build()
    installAiGateway(runtime, config)
}

fun Routing.installAiGateway(
    runtime: GatewayRuntime,
    config: AiGatewayKtorConfig
) {
    if (config.pathPrefix.isBlank()) {
        installProviderRoutes(runtime, config)
    } else {
        route(config.pathPrefix) {
            installProviderRoutes(runtime, config)
        }
    }
}

private fun Route.installProviderRoutes(
    runtime: GatewayRuntime,
    config: AiGatewayKtorConfig
) {
        val openAiAdapter = runtime.inbounds.openAi
        if (config.installOpenAi && openAiAdapter != null) {
            post(config.openAiPath) {
                val request = call.parseBodyOrNull<OpenAiChatCompletionsRequest>(config.json).respondIfNull(call) ?: return@post
                val metadata = call.bindRequestMetadata(config.metadataBinder)
                call.handleGatewayResponse(
                    json = config.json,
                    stream = call.isStreamRequested(request.stream),
                    onStream = { openAiAdapter.generateStream(request, metadata) },
                    onUnary = { openAiAdapter.generate(request, metadata) }
                )
            }
        }

        val anthropicAdapter = runtime.inbounds.anthropic
        if (config.installAnthropic && anthropicAdapter != null) {
            post(config.anthropicPath) {
                val request = call.parseBodyOrNull<AnthropicMessagesRequest>(config.json).respondIfNull(call) ?: return@post
                val metadata = call.bindRequestMetadata(config.metadataBinder)
                call.handleGatewayResponse(
                    json = config.json,
                    stream = call.isStreamRequested(request.stream),
                    onStream = { anthropicAdapter.generateStream(request, metadata) },
                    onUnary = { anthropicAdapter.generate(request, metadata) }
                )
            }
        }

        val geminiAdapter = runtime.inbounds.gemini
        if (config.installGemini && geminiAdapter != null) {
            post(config.geminiPath) {
                val request = call.parseBodyOrNull<GeminiGenerateContentRequest>(config.json).respondIfNull(call) ?: return@post
                val metadata = call.bindRequestMetadata(config.metadataBinder)
                call.handleGatewayResponse(
                    json = config.json,
                    stream = call.isStreamRequested(explicitFlag = null),
                    onStream = { geminiAdapter.generateStream(request, metadata) },
                    onUnary = { geminiAdapter.generate(request, metadata) }
                )
            }
        }
}

fun Routing.ktorServerAdapter(
    configure: AiGatewayKtorConfigDsl.() -> Unit = {}
): GatewayNetworkAdapter = GatewayNetworkAdapter { runtime ->
    installAiGateway(runtime, configure)
}

private fun normalizePrefix(prefix: String): String {
    if (prefix.isBlank()) return ""
    val startsWithSlash = prefix.startsWith('/')
    return if (startsWithSlash) prefix else "/$prefix"
}

private fun ApplicationCall.bindRequestMetadata(binder: ConfigurableMetadataBinder): TypedMap {
    val clientIp = extractClientIp()
    val context = KtorIncomingContext(this, clientIp)
    return binder.bind(context).also { typedMap ->
        // Keeps IP consistently available even when a custom binder omits it.
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

private suspend inline fun <reified T : Any> ApplicationCall.handleGatewayResponse(
    json: Json,
    stream: Boolean,
    crossinline onStream: suspend () -> Either<DomainError, Flow<T>>,
    crossinline onUnary: suspend () -> Either<DomainError, T>
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
        respond(HttpStatusCode.InternalServerError, mapOf("error" to (throwable.message ?: "Internal server error")))
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
            write("data: $payload\\n\\n")
            flush()
        }
    }
}

private suspend fun ApplicationCall.respondDomainError(error: DomainError) {
    val status = when (error) {
        is InvalidRequest, is ParsingError -> HttpStatusCode.BadRequest
        is ApiDownError -> HttpStatusCode.ServiceUnavailable
        is ProviderApiError -> HttpStatusCode.BadGateway
        else -> HttpStatusCode.InternalServerError
    }

    respond(status, mapOf("error" to error.message))
}

private fun ApplicationCall.isStreamRequested(explicitFlag: Boolean?): Boolean =
    explicitFlag ?: request.queryParameters["stream"]?.toBooleanStrictOrNull() ?: false




