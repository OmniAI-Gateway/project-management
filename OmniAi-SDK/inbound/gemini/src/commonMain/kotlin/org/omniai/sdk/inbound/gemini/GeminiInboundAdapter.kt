package org.omniai.sdk.inbound.gemini

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.contracts.gemini.input.GeminiGenerateContentRequest
import org.omniai.sdk.contracts.gemini.output.GeminiGenerateContentResponse
import org.omniai.sdk.common.Either
import org.omniai.sdk.common.Failure
import org.omniai.sdk.common.Success
import org.omniai.sdk.common.TypedMap
import org.omniai.sdk.common.failure
import org.omniai.sdk.common.success
import org.omniai.sdk.ports.inbound.InboundPort
import org.omniai.sdk.ports.inbound.DispatcherPort
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.requests.CommonRequest

class GeminiInboundAdapter(
    private val dispatcher: DispatcherPort,
    private val translator: GeminiInboundTranslator = GeminiInboundTranslator()
) : InboundPort<GeminiGenerateContentRequest, GeminiGenerateContentResponse, GeminiGenerateContentResponse> {

    companion object {
        const val GEMINI_MODEL_KEY: String = "gemini.model"
    }
    override val provider: Provider = Provider.GEMINI

    override suspend fun generate(
        request: GeminiGenerateContentRequest,
        map: TypedMap,
    ): Either<DomainError, GeminiGenerateContentResponse> {
        val domainRequest = translator.toDomain(request).withModelOverride(map).also {
            it.providerOptions.putAll(map)
        }
        return when (val domainResponse = dispatcher.generate(domainRequest, map)) {
            is Success-> success(translator.fromDomain(domainResponse.value))
            is Failure -> failure(domainResponse.value)
        }
    }

    override suspend fun generateStream(
        request: GeminiGenerateContentRequest,
        map: TypedMap,
    ): Either<DomainError, Flow<GeminiGenerateContentResponse>> {
        val domainRequest = translator.toDomain(request).withModelOverride(map).also {
            it.providerOptions.putAll(map)
        }
        return when (val streamResult = dispatcher.generateStream(domainRequest, map)) {
            is Success -> success(translator.fromDomainEvent(streamResult.value))
            is Failure -> failure(streamResult.value)
        }
    }
}

private fun CommonRequest.withModelOverride(map: TypedMap): CommonRequest {
    val modelOverride: String? = map[GeminiInboundAdapter.GEMINI_MODEL_KEY]
    return if (modelOverride.isNullOrBlank()) this else copy(model = modelOverride)
}
