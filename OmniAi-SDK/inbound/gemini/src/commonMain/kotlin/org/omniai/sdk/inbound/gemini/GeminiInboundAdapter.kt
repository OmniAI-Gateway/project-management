package org.omniai.sdk.inbound.gemini

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.contracts.gemini.input.GeminiGenerateContentRequest
import org.omniai.sdk.contracts.gemini.output.GeminiGenerateContentResponse
import org.omniai.sdk.core.commom.Either
import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.core.commom.failure
import org.omniai.sdk.core.commom.success
import org.omniai.sdk.core.ports.InboundPort
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.requests.CommonRequest

const val GEMINI_MODEL_KEY: String = "gemini.model"

class GeminiInboundAdapter(
    private val service: InferenceServicePort,
    private val translator: GeminiInboundTranslator = GeminiInboundTranslator()
) : InboundPort<GeminiGenerateContentRequest, GeminiGenerateContentResponse, GeminiGenerateContentResponse> {

    override val provider: Provider = Provider.GEMINI

    override suspend fun generate(
        request: GeminiGenerateContentRequest,
        map: TypedMap,
    ): Either<DomainError, GeminiGenerateContentResponse> {
        val domainRequest = translator.toDomain(request).withModelOverride(map)
        return when (val domainResponse = service.generate(domainRequest)) {
            is Either.Right -> success(translator.fromDomain(domainResponse.value))
            is Either.Left -> failure(domainResponse.value)
        }
    }

    override suspend fun generateStream(
        request: GeminiGenerateContentRequest,
        map: TypedMap,
    ): Either<DomainError, Flow<GeminiGenerateContentResponse>> {
        val domainRequest = translator.toDomain(request).withModelOverride(map)
        return when (val streamResult = service.generateStream(domainRequest)) {
            is Either.Right -> success(translator.fromDomainEvent(streamResult.value))
            is Either.Left -> failure(streamResult.value)
        }
    }
}

private fun CommonRequest.withModelOverride(map: TypedMap): CommonRequest {
    val modelOverride: String? = map[GEMINI_MODEL_KEY]
    return if (modelOverride.isNullOrBlank()) this else copy(model = modelOverride)
}
