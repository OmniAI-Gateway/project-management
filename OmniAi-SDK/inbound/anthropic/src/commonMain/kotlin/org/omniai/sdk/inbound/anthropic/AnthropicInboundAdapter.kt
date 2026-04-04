package org.omniai.sdk.inbound.anthropic

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.output.AnthropicMessageResponse
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent
import org.omniai.sdk.core.commom.Either
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
        val domainRequest = translator.toDomain(request)
        return when (val domainResponse = service.generate(domainRequest)) {
            is Either.Right -> success(translator.fromDomain(domainResponse.value))
            is Either.Left -> failure(domainResponse.value)
        }
    }

    override suspend fun generateStream(
        request: AnthropicMessagesRequest,
        map: TypedMap,
    ): Either<DomainError, Flow<AnthropicStreamEvent>> {
        val domainRequest = translator.toDomain(request)
        return when (val streamResult = service.generateStream(domainRequest)) {
            is Either.Right -> success(translator.fromDomainEvent(streamResult.value))
            is Either.Left -> failure(streamResult.value)
        }
    }
}
