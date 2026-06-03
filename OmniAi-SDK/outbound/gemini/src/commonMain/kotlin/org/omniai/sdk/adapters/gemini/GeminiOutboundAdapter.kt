package org.omniai.sdk.adapters.gemini

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import org.omniai.sdk.contracts.gemini.output.GeminiError
import org.omniai.sdk.contracts.gemini.output.GeminiErrorResponse
import org.omniai.sdk.contracts.gemini.input.GeminiGenerateContentRequest
import org.omniai.sdk.contracts.gemini.output.GeminiEventStream
import org.omniai.sdk.contracts.gemini.output.GeminiGenerateContentResponse
import org.omniai.sdk.common.AttributeKey
import org.omniai.sdk.common.Either
import org.omniai.sdk.common.TypedMap
import org.omniai.sdk.common.failure
import org.omniai.sdk.common.success
import org.omniai.sdk.ports.outbound.http.HttpCallResult
import org.omniai.sdk.ports.outbound.http.HttpMethod
import org.omniai.sdk.ports.outbound.http.HttpTransportClient
import org.omniai.sdk.ports.outbound.http.RequestConfig
import org.omniai.sdk.ports.outbound.http.defaultHttpTransportClient
import org.omniai.sdk.ports.outbound.http.executeRequest
import org.omniai.sdk.ports.outbound.http.listenEvents
import org.omniai.sdk.ports.outbound.http.requestConfig
import org.omniai.sdk.ports.outbound.http.toDomainError
import org.omniai.sdk.ports.outbound.OutboundPort
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

class GeminiOutboundAdapter(
    override val model: Model,
    private val apiKey: String,
    private val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta",
    private val transportClient: HttpTransportClient = defaultHttpTransportClient(),
    key: String? = null
) : OutboundPort {

    private val translator = GeminiOutboundTranslator()

    override val provider: Provider = Provider.GEMINI

    override val key: String = key ?: "${provider.value}-${model.model}-${apiKey.takeLast(4)}"

    override suspend fun generate(request: CommonRequest): Either<DomainError, CommonResponse> {
        val providerRequest = translator.fromDomain(request)
        val requestConfig = requestConfig(
            url = "$baseUrl/models/{model}:generateContent"
        ) {
            pathParam("model", request.model)
            method = HttpMethod.POST
            header("x-goog-api-key", apiKey)
            header("content-type", "application/json")
            parameter("key", apiKey)
            body = providerRequest
        }

        val callResult = transportClient.executeRequest<GeminiGenerateContentResponse, GeminiGenerateContentRequest>(requestConfig)

        return when (callResult) {
            is HttpCallResult.Success -> {
                val translated = translator.toDomain(callResult.data)
                success(translated.withHttpMetadata(callResult.metadata))
            }
            else -> failure(callResult.toDomainError(provider))
        }
    }

    override suspend fun generateStream(request: CommonRequest): Either<DomainError, Flow<CommonResponseEvent>> {
        val providerRequest = translator.fromDomain(request)
        val requestConfig = providerRequest.toStreamPost(request.model)

        val providerEventFlow = transportClient
            .listenEvents<GeminiGenerateContentResponse, GeminiGenerateContentRequest>(
                config = requestConfig,
                eventName = null
            )
            .map { callResult ->
                when (callResult) {
                    is HttpCallResult.Success -> GeminiEventStream.Chunk(callResult.data)
                    is HttpCallResult.NetworkError -> GeminiEventStream.Error(
                        GeminiErrorResponse(
                            error = GeminiError(
                                code = -1,
                                message = callResult.exception.message ?: "Network error",
                                status = "UNAVAILABLE",
                                details = emptyList()
                            )
                        )
                    )
                    is HttpCallResult.ApiError -> GeminiEventStream.Error(
                        GeminiErrorResponse(
                            error = GeminiError(
                                code = callResult.code,
                                message = callResult.message ?: "API error",
                                status = "API_ERROR",
                                details = emptyList()
                            )
                        )
                    )
                    is HttpCallResult.SerializationError -> GeminiEventStream.Error(
                        GeminiErrorResponse(
                            error = GeminiError(
                                code = -2,
                                message = callResult.exception.message ?: "Serialization error",
                                status = "INTERNAL",
                                details = emptyList()
                            )
                        )
                    )
                    is HttpCallResult.UnknownError -> GeminiEventStream.Error(
                        GeminiErrorResponse(
                            error = GeminiError(
                                code = -3,
                                message = callResult.exception.message ?: "Unknown error",
                                status = "INTERNAL",
                                details = emptyList()
                            )
                        )
                    )
                }
            }
            .onCompletion { cause ->
                if (cause == null) {
                    emit(GeminiEventStream.Done)
                }
            }

        val eventFlow = translator.toDomainEvent(providerEventFlow)

        return success(eventFlow)
    }

    private fun GeminiGenerateContentRequest.toStreamPost(model: String): RequestConfig<GeminiGenerateContentRequest> =
        requestConfig(url = "$baseUrl/models/{model}:streamGenerateContent") {
            pathParam("model", model)
            method = HttpMethod.POST
            header("x-goog-api-key", apiKey)
            header("content-type", "application/json")
            parameter("key", apiKey)
            parameter("alt", "sse")
            body = this@toStreamPost
        }
}

private fun CommonResponse.withHttpMetadata(metadata: TypedMap): CommonResponse {
    val metadataMap = metadata.toUntypedMap()
    if (metadataMap.isEmpty()) return this
    return copy(providerOptions = providerOptions + metadataMap)
}

private fun TypedMap.toUntypedMap(): Map<String, Any?> =
    keys().associate { key -> key.name to getUnsafe(key) }

@Suppress("UNCHECKED_CAST")
private fun TypedMap.getUnsafe(key: AttributeKey<*>): Any? =
    this[key as AttributeKey<Any>]

