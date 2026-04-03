package org.omniai.sdk.inbound.gemini

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.omniai.sdk.contracts.gemini.input.GeminiGenerateContentRequest
import org.omniai.sdk.contracts.gemini.output.GeminiGenerateContentResponse
import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.core.ports.InboundPort
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.requests.CommonRequest

const val GEMINI_MODEL_KEY: String = "gemini.model"

class GeminiInboundAdapter(
    private val service: InferenceServicePort,
    private val translator: GeminiInboundTranslator = GeminiInboundTranslator()
) : InboundPort<GeminiGenerateContentRequest, GeminiGenerateContentResponse, GeminiGenerateContentResponse> {

    override val provider: Provider = Provider.GEMINI

    override suspend fun generate(request: GeminiGenerateContentRequest, map: TypedMap): GeminiGenerateContentResponse {
        val domainRequest = translator.toDomain(request).withModelOverride(map)
        val domainResponse = service.generate(domainRequest)
        return translator.fromDomain(domainResponse)
    }

    override fun generateStream(request: GeminiGenerateContentRequest, map: TypedMap): Flow<GeminiGenerateContentResponse> {
        val domainRequest = translator.toDomain(request).withModelOverride(map)
        return service.generateStream(domainRequest).map(translator::fromDomainEvent)
    }
}

private fun CommonRequest.withModelOverride(map: TypedMap): CommonRequest {
    val modelOverride: String? = map[GEMINI_MODEL_KEY]
    return if (modelOverride.isNullOrBlank()) this else copy(model = modelOverride)
}

