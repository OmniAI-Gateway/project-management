package org.omniai.sdk.inbound.anthropic

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.output.AnthropicMessageResponse
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent
import org.omniai.sdk.core.ports.InboundPort
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.domain.common.Provider

class AnthropicInboundAdapter(
    private val service: InferenceServicePort,
    private val translator: AnthropicInboundTranslator = AnthropicInboundTranslator()
) : InboundPort<AnthropicMessagesRequest, AnthropicMessageResponse, AnthropicStreamEvent> {

    override val provider: Provider = Provider.ANTHROPIC

    override suspend fun generate(request: AnthropicMessagesRequest): AnthropicMessageResponse {
        val domainRequest = translator.toDomain(request)
        val domainResponse = service.generate(domainRequest)
        return translator.fromDomain(domainResponse)
    }

    override fun generateStream(request: AnthropicMessagesRequest): Flow<AnthropicStreamEvent> {
        val domainRequest = translator.toDomain(request)
        return service.generateStream(domainRequest).map(translator::fromDomainEvent)
    }
}
