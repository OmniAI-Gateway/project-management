package org.omniai.sdk.adapters.openai

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.contracts.openai.output.OpenAiChatCompletionsResponse
import org.omniai.sdk.core.commom.Either
import org.omniai.sdk.core.commom.failure
import org.omniai.sdk.core.commom.success
import org.omniai.sdk.core.http.HttpCallResult
import org.omniai.sdk.core.http.HttpMethod
import org.omniai.sdk.core.http.HttpTransportClient
import org.omniai.sdk.core.http.defaultHttpTransportClient
import org.omniai.sdk.core.http.executeRequest
import org.omniai.sdk.core.http.requestConfig
import org.omniai.sdk.core.http.toDomainError
import org.omniai.sdk.core.ports.OutboundPort
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.errors.InvalidRequest
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

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

        val callResult = transportClient.executeRequest<OpenAiChatCompletionsResponse, _>(requestConfig)

        return when (callResult) {
            is HttpCallResult.Success -> success(translator.toDomain(callResult.data))
            else -> failure(callResult.toDomainError(provider))
        }
    }

    override suspend fun generateStream(request: CommonRequest): Either<DomainError, Flow<CommonResponseEvent>> {
        return failure(InvalidRequest("OpenAI stream generation is not implemented yet"))
    }
}
