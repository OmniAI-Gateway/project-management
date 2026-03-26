package org.omniai.sdk.adapters.openai

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.omniai.sdk.contracts.openai.output.OpenAiChatCompletionsResponse
import org.omniai.sdk.core.http.HttpCallResult
import org.omniai.sdk.core.http.HttpMethod
import org.omniai.sdk.core.http.HttpTransportClient
import org.omniai.sdk.core.http.executeRequest
import org.omniai.sdk.core.http.requestConfig
import org.omniai.sdk.core.ports.OutboundPort
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

class OpenAiOutboundAdapter(
    override val model: Model,
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1",
    private val transportClient: HttpTransportClient = KtorHttpTransportClient.default()
) : OutboundPort {

    private val translator = OpenAiOutboundTranslator()

    override val provider: Provider = Provider.OPENAI

    override suspend fun generate(request: CommonRequest): CommonResponse {
        val providerRequest = translator.fromDomain(request)
        val requestConfig = requestConfig(url = "$baseUrl/chat/completions") {
            method = HttpMethod.POST
            header("Authorization", "Bearer $apiKey")
            header("Content-Type", "application/json")
            body = providerRequest
        }

        println("REQUEST: ${Json.encodeToJsonElement(providerRequest)}")
        val callResult = transportClient.executeRequest<OpenAiChatCompletionsResponse, _>(requestConfig)

        val providerResponse = when (callResult) {
            is HttpCallResult.Success -> callResult.data
            is HttpCallResult.ApiError -> throw IllegalStateException(
                "OpenAI request failed with status ${callResult.code}. body=${callResult.message.orEmpty()}"
            )
            is HttpCallResult.NetworkError -> throw IllegalStateException(
                "OpenAI request failed due to network error",
                callResult.exception
            )
            is HttpCallResult.SerializationError -> throw IllegalStateException(
                "OpenAI request failed while decoding the response",
                callResult.exception
            )
            is HttpCallResult.UnknownError -> throw IllegalStateException(
                "OpenAI request failed with unknown error",
                callResult.exception
            )
        }

        println("RESPONSE: ${Json.encodeToJsonElement(providerResponse)}")
        return translator.toDomain(providerResponse)
    }

    override fun generateStream(request: CommonRequest): Flow<CommonResponseEvent> {
        TODO("Not yet implemented")
    }

}
