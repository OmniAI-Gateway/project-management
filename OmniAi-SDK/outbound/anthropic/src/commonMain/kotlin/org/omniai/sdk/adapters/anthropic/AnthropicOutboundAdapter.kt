package org.omniai.sdk.adapters.anthropic

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import org.omniai.sdk.contracts.anthropic.output.AnthropicError
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.output.AnthropicMessageResponse
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent
import org.omniai.sdk.core.commom.Either
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

class AnthropicOutboundAdapter(
    override val model: Model,
    private val apiKey: String,
    private val baseUrl: String = "https://api.anthropic.com/v1",
    private val transportClient: HttpTransportClient = defaultHttpTransportClient(),
    private val anthropicVersion: String = "2023-06-01"
) : OutboundPort {

    private val translator = AnthropicOutboundTranslator()

    override val provider: Provider = Provider.ANTHROPIC

    override suspend fun generate(request: CommonRequest): Either<DomainError, CommonResponse> {
        val providerRequest = translator.fromDomain(request)
        val requestConfig = providerRequest.toSimplePost()
        val callResult = transportClient.executeRequest<AnthropicMessageResponse, AnthropicMessagesRequest>(requestConfig)

        return when (callResult) {
            is HttpCallResult.Success -> success(translator.toDomain(callResult.data))
            else -> failure(callResult.toDomainError(provider))
        }
    }

    override suspend fun generateStream(request: CommonRequest): Either<DomainError, Flow<CommonResponseEvent>> {
        val providerRequest = translator.fromDomain(request).copy(stream = true)
        val requestConfig = providerRequest.toSimplePost()
        val providerEventFlow: Flow<AnthropicStreamEvent> = transportClient
            .listenEvents<AnthropicStreamEvent, AnthropicMessagesRequest>(config = requestConfig) {
            on<AnthropicStreamEvent.MessageStart>("message_start")
            on<AnthropicStreamEvent.ContentBlockStart>("content_block_start")
            on<AnthropicStreamEvent.ContentBlockDelta>("content_block_delta")
            on<AnthropicStreamEvent.ContentBlockStop>("content_block_stop")
            on<AnthropicStreamEvent.MessageDelta>("message_delta")
            on<AnthropicStreamEvent.MessageStop>("message_stop")
            on<AnthropicStreamEvent.Ping>("ping")
            on<AnthropicStreamEvent.Error>("error")
            }
            .map { callResult ->
                when (callResult) {
                    is HttpCallResult.Success -> callResult.data
                    is HttpCallResult.NetworkError -> AnthropicStreamEvent.Error(
                        error = AnthropicError(
                            type = "network_error",
                            message = callResult.exception.message ?: "Network error"
                        )
                    )
                    is HttpCallResult.ApiError -> AnthropicStreamEvent.Error(
                        error = AnthropicError(
                            type = "api_error",
                            message = callResult.message ?: "API error"
                        )
                    )
                    is HttpCallResult.SerializationError -> AnthropicStreamEvent.Error(
                        error = AnthropicError(
                            type = "serialization_error",
                            message = callResult.exception.message ?: "Serialization error"
                        )
                    )
                    is HttpCallResult.UnknownError -> AnthropicStreamEvent.Error(
                        error = AnthropicError(
                            type = "unknown_error",
                            message = callResult.exception.message ?: "Unknown error"
                        )
                    )
                }
            }
            .onCompletion { cause ->
                if (cause == null) {
                    emit(AnthropicStreamEvent.MessageStop)
                }
            }

        val eventFlow = translator.toDomainEvent(providerEventFlow)

        return success(eventFlow)
    }

    private fun AnthropicMessagesRequest.toSimplePost(): RequestConfig<AnthropicMessagesRequest> =
        requestConfig(url = "$baseUrl/messages") {
            method = HttpMethod.POST
            header("x-api-key", apiKey)
            header("anthropic-version", anthropicVersion)
            header("content-type", "application/json")
            body = this@toSimplePost
        }

}