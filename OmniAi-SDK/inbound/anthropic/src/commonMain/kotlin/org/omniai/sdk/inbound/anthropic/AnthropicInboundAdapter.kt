package org.omniai.sdk.inbound.anthropic

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.common.Either
import org.omniai.sdk.common.Failure
import org.omniai.sdk.common.Success
import org.omniai.sdk.common.TypedMap
import org.omniai.sdk.common.failure
import org.omniai.sdk.common.success
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.output.AnthropicMessageResponse
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.ports.inbound.DispatcherPort
import org.omniai.sdk.ports.inbound.InboundPort

class AnthropicInboundAdapter(
    private val dispatcher: DispatcherPort,
    private val translator: AnthropicInboundTranslator = AnthropicInboundTranslator(),
) : InboundPort<AnthropicMessagesRequest, AnthropicMessageResponse, AnthropicStreamEvent> {
    override val provider: Provider = Provider.ANTHROPIC

    override suspend fun generate(
        request: AnthropicMessagesRequest,
        map: TypedMap,
    ): Either<DomainError, AnthropicMessageResponse> {
        val domainRequest =
            translator.toDomain(request).also {
                it.providerOptions.putAll(map)
            }
        return when (val domainResponse = dispatcher.generate(domainRequest, map)) {
            is Success -> success(translator.fromDomain(domainResponse.value))
            is Failure -> failure(domainResponse.value)
        }
    }

    override suspend fun generateStream(
        request: AnthropicMessagesRequest,
        map: TypedMap,
    ): Either<DomainError, Flow<AnthropicStreamEvent>> {
        val domainRequest =
            translator.toDomain(request).also {
                it.providerOptions.putAll(map)
            }
        return when (val streamResult = dispatcher.generateStream(domainRequest, map)) {
            is Success -> success(translator.fromDomainEvent(streamResult.value))
            is Failure -> failure(streamResult.value)
        }
    }
}
