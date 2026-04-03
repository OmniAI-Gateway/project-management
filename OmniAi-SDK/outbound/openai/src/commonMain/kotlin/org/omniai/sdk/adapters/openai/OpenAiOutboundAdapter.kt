package org.omniai.sdk.adapters.openai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import org.omniai.sdk.contracts.openai.input.OpenAiChatCompletionsRequest
import org.omniai.sdk.contracts.openai.output.OpenAiEventStream
import org.omniai.sdk.contracts.openai.output.OpenAiChatCompletionsResponse
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
import org.omniai.sdk.domain.responses.ResponseErrored

class OpenAiOutboundAdapter(
    override val model: Model,
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1",
    private val transportClient: HttpTransportClient = defaultHttpTransportClient()
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
        return when (val callResult = transportClient.
            executeRequest<OpenAiChatCompletionsResponse,OpenAiChatCompletionsRequest>(requestConfig)) {
            is HttpCallResult.Success -> success(translator.toDomain(callResult.data))
            else -> failure(callResult.toDomainError(provider))
        }
    }

    override suspend fun generateStream(request: CommonRequest): Either<DomainError, Flow<CommonResponseEvent>> {
        val providerRequest = translator.fromDomain(request).copy(stream = true)
        val requestConfig = providerRequest.toSimplePost()

        val eventFlow = transportClient
            .listenEvents<OpenAiChatCompletionsResponse, OpenAiChatCompletionsRequest>(
                config = requestConfig,
                eventName = null
            )
            .map { callResult ->
                when (callResult) {
                    is HttpCallResult.Success -> translator.toDomainEvent(OpenAiEventStream.Chunk(callResult.data))
                    is HttpCallResult.SerializationError -> {
                        if (callResult.exception.message?.contains("[DONE]", ignoreCase = true) == true) {
                            translator.toDomainEvent(OpenAiEventStream.Done)
                        } else {
                            ResponseErrored(
                                provider = provider,
                                model = model,
                                sequence = 0,
                                message = callResult.toDomainError(provider).message,
                                retryable = false,
                                providerEventType = "transport_error"
                            )
                        }
                    }
                    else -> ResponseErrored(
                        provider = provider,
                        model = model,
                        sequence = 0,
                        message = callResult.toDomainError(provider).message,
                        retryable = callResult is HttpCallResult.NetworkError ||
                            (callResult is HttpCallResult.ApiError && callResult.code in 500..599),
                        providerEventType = "transport_error"
                    )
                }
            }
            .onCompletion { cause ->
                if (cause == null) {
                    emit(translator.toDomainEvent(OpenAiEventStream.Done))
                }
            }

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
