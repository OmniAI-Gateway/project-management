package org.omniai.sdk.adapters.anthropic

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.output.AnthropicMessageResponse
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
        val requestConfig = requestConfig<AnthropicMessagesRequest>(url = "$baseUrl/messages") {
            method = HttpMethod.POST
            header("x-api-key", apiKey)
            header("anthropic-version", anthropicVersion)
            header("content-type", "application/json")
            body = providerRequest
        }

        val callResult = transportClient.executeRequest<AnthropicMessageResponse, AnthropicMessagesRequest>(requestConfig)

        return when (callResult) {
            is HttpCallResult.Success -> success(translator.toDomain(callResult.data))
            else -> failure(callResult.toDomainError(provider))
        }
    }

    override suspend fun generateStream(request: CommonRequest): Either<DomainError, Flow<CommonResponseEvent>> {
        return failure(InvalidRequest("Anthropic stream generation is not implemented yet"))
    }
}