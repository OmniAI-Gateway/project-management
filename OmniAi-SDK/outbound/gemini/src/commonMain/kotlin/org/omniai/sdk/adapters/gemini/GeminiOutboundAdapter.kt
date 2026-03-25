package org.omniai.sdk.adapters.gemini

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.contracts.gemini.input.GeminiGenerateContentRequest
import org.omniai.sdk.contracts.gemini.output.GeminiGenerateContentResponse
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

class GeminiOutboundAdapter(
	override val model: Model,
	private val apiKey: String,
	private val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta",
	private val transportClient: HttpTransportClient = defaultTransportClient()
) : OutboundPort {

	private val translator = GeminiOutboundTranslator()

	override val provider: Provider = Provider.GEMINI

	override suspend fun generate(request: CommonRequest): CommonResponse {
		val providerRequest = translator.fromDomain(request)
		val requestConfig = requestConfig(
			url = "$baseUrl/models/${request.model}:generateContent"
		) {
			method = HttpMethod.POST
			header("x-goog-api-key", apiKey)
			header("content-type", "application/json")
			parameter("key", apiKey)
			body = providerRequest
		}

		val callResult = transportClient.executeRequest<GeminiGenerateContentResponse, GeminiGenerateContentRequest>(requestConfig)

		val providerResponse = when (callResult) {
			is HttpCallResult.Success -> callResult.data
			is HttpCallResult.ApiError -> throw IllegalStateException(
				"Gemini request failed with status ${callResult.code}. body=${callResult.message.orEmpty()}"
			)
			is HttpCallResult.NetworkError -> throw IllegalStateException(
				"Gemini request failed due to network error",
				callResult.exception
			)
			is HttpCallResult.SerializationError -> throw IllegalStateException(
				"Gemini request failed while decoding the response",
				callResult.exception
			)
			is HttpCallResult.UnknownError -> throw IllegalStateException(
				"Gemini request failed with unknown error",
				callResult.exception
			)
		}

		return translator.toDomain(providerResponse)
	}

	override fun generateStream(request: CommonRequest): Flow<CommonResponseEvent> {
		TODO("Not yet implemented")
	}
}
