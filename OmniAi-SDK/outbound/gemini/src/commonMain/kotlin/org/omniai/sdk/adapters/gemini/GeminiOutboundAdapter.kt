package org.omniai.sdk.adapters.gemini

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import org.omniai.sdk.contracts.gemini.input.GeminiGenerateContentRequest
import org.omniai.sdk.contracts.gemini.output.GeminiEventStream
import org.omniai.sdk.contracts.gemini.output.GeminiGenerateContentResponse
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

class GeminiOutboundAdapter(
    override val model: Model,
    private val apiKey: String,
    private val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta",
    private val transportClient: HttpTransportClient = defaultHttpTransportClient()
) : OutboundPort {

    private val translator = GeminiOutboundTranslator()

    override val provider: Provider = Provider.GEMINI

    override suspend fun generate(request: CommonRequest): Either<DomainError, CommonResponse> {
        val providerRequest = translator.fromDomain(request)
        val requestConfig = requestConfig(
            url = "$baseUrl/models/{model}"
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
            is HttpCallResult.Success -> success(translator.toDomain(callResult.data))
            else -> failure(callResult.toDomainError(provider))
        }
    }

    override suspend fun generateStream(request: CommonRequest): Either<DomainError, Flow<CommonResponseEvent>> {
        val providerRequest = translator.fromDomain(request)
        val requestConfig = providerRequest.toStreamPost(request.model)

        val eventFlow = transportClient
            .listenEvents<GeminiGenerateContentResponse, GeminiGenerateContentRequest>(
                config = requestConfig,
                eventName = null
            )
            .map { callResult ->
                when (callResult) {
                    is HttpCallResult.Success -> translator.toDomainEvent(GeminiEventStream.Chunk(callResult.data))
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
                    emit(translator.toDomainEvent(GeminiEventStream.Done))
                }
            }

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
