package org.omniai.sdk.adapters.openai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import org.omniai.sdk.binders.ConfigurableMetadataBinder
import org.omniai.sdk.core.commom.AttributeKey
import org.omniai.sdk.contracts.openai.input.OpenAiChatCompletionsRequest
import org.omniai.sdk.contracts.openai.output.OpenAiChatCompletionsResponse
import org.omniai.sdk.contracts.openai.output.OpenAiError
import org.omniai.sdk.contracts.openai.output.OpenAiEventStream
import org.omniai.sdk.core.commom.Either
import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.core.commom.failure
import org.omniai.sdk.core.commom.success
import org.omniai.sdk.core.http.HttpCallResult
import org.omniai.sdk.core.http.HttpMethod
import org.omniai.sdk.core.http.HttpTransportClient
import org.omniai.sdk.core.http.RequestConfig
import org.omniai.sdk.core.http.defaultHttpTransportClient
import org.omniai.sdk.core.http.executeRequest
import org.omniai.sdk.core.http.listenEvents
import org.omniai.sdk.core.http.requestConfig
import org.omniai.sdk.core.http.toDomainError
import org.omniai.sdk.core.ports.OutboundPort
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

class OpenAiOutboundAdapter(
    override val model: Model,
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1",
    private val transportClient: HttpTransportClient = defaultHttpTransportClient(),
) : OutboundPort {

    private val translator = OpenAiOutboundTranslator()

    override val provider: Provider = Provider.OPENAI

    override suspend fun generate(request: CommonRequest): Either<DomainError, CommonResponse> {
        val providerRequest = translator.fromDomain(request)
        val requestConfig = requestConfig(url = "$baseUrl/chat/completions") {
            method = HttpMethod.POST
            header("Authorization", "Bearer $apiKey")
            header("Content-Type", "application/json")
            body = providerRequest
        }
        val callResult = transportClient.executeRequest<OpenAiChatCompletionsResponse, OpenAiChatCompletionsRequest>(requestConfig)
        return when (callResult) {
            is HttpCallResult.Success -> {
                val translated = translator.toDomain(callResult.data)
                success(translated.withHttpMetadata(callResult.metadata))
            }
            else -> failure(callResult.toDomainError(provider))
        }
    }

    override suspend fun generateStream(request: CommonRequest): Either<DomainError, Flow<CommonResponseEvent>> {
        val providerRequest = translator.fromDomain(request).copy(stream = true)
        val requestConfig = providerRequest.toSimplePost()

        val providerEventFlow: Flow<OpenAiEventStream> = transportClient
            .listenEvents<OpenAiChatCompletionsResponse, OpenAiChatCompletionsRequest>(
                config = requestConfig,
                eventName = null
            )
            .map { callResult ->
                when (callResult) {
                    is HttpCallResult.Success -> OpenAiEventStream.Chunk(callResult.data)
                    is HttpCallResult.NetworkError -> OpenAiEventStream.Error(
                        OpenAiError(
                            message = callResult.exception.message ?: "Network error",
                            type = "network_error"
                        )
                    )
                    is HttpCallResult.ApiError -> OpenAiEventStream.Error(
                        OpenAiError(
                            message = callResult.message ?: "API error",
                            type = "api_error",
                            code = callResult.code.toString()
                        )
                    )
                    is HttpCallResult.SerializationError -> if (
                        callResult.exception.message?.contains("[DONE]") == true
                    ) {
                        OpenAiEventStream.Done
                    } else {
                        OpenAiEventStream.Error(
                            OpenAiError(
                                message = callResult.exception.message ?: "Serialization error",
                                type = "serialization_error"
                            )
                        )
                    }
                    is HttpCallResult.UnknownError -> OpenAiEventStream.Error(
                        OpenAiError(
                            message = callResult.exception.message ?: "Unknown error",
                            type = "unknown_error"
                        )
                    )
                }
            }
            .onCompletion { cause ->
                if (cause == null) {
                    emit(OpenAiEventStream.Done)
                }
            }

        val eventFlow = translator.toDomainEvent(providerEventFlow)
        return success(eventFlow)
    }


    private fun OpenAiChatCompletionsRequest.toSimplePost(): RequestConfig<OpenAiChatCompletionsRequest> =
        requestConfig(url = "$baseUrl/chat/completions") {
            method = HttpMethod.POST
            header("Authorization", "Bearer $apiKey")
            header("Content-Type", "application/json")
            body = this@toSimplePost
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

