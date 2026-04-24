package org.omniai.sdk.inbound.anthropic

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.output.AnthropicMessageResponse
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent
import org.omniai.sdk.core.commom.Either
import org.omniai.sdk.core.commom.Failure
import org.omniai.sdk.core.commom.Success
import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.core.commom.failure
import org.omniai.sdk.core.commom.success
import org.omniai.sdk.core.ports.InboundPort
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.errors.DomainError

class AnthropicInboundAdapter(
    private val service: InferenceServicePort,
    private val translator: AnthropicInboundTranslator = AnthropicInboundTranslator()
) : InboundPort<AnthropicMessagesRequest, AnthropicMessageResponse, AnthropicStreamEvent> {

    override val provider: Provider = Provider.ANTHROPIC

    override suspend fun generate(
        request: AnthropicMessagesRequest,
        map: TypedMap,
    ): Either<DomainError, AnthropicMessageResponse> {
        val domainRequest = translator.toDomain(request).also {
            it.providerOptions.putAll(map)
        }
        return when (val domainResponse = service.generate(domainRequest, map)) {
            is Success -> success(translator.fromDomain(domainResponse.value))
            is Failure -> failure(domainResponse.value)
        }
    }

    override suspend fun generateStream(
        request: AnthropicMessagesRequest,
        map: TypedMap,
    ): Either<DomainError, Flow<AnthropicStreamEvent>> {
        val domainRequest = translator.toDomain(request).also {
            it.providerOptions.putAll(map)
        }
        return when (val streamResult = service.generateStream(domainRequest, map)) {
            is Success -> success(translator.fromDomainEvent(streamResult.value))
            is Failure -> failure(streamResult.value)
        }
    }
}
