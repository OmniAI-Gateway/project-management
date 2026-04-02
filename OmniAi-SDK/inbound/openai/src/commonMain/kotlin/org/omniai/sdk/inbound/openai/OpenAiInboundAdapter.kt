package org.omniai.sdk.inbound.openai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.omniai.sdk.contracts.openai.input.OpenAiChatCompletionsRequest
import org.omniai.sdk.contracts.openai.output.OpenAiChatCompletionsResponse
import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.core.ports.InboundPort
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.domain.common.Provider

class OpenAiInboundAdapter(
    private val service: InferenceServicePort,
    private val translator: OpenAiInboundTranslator = OpenAiInboundTranslator()
) : InboundPort<OpenAiChatCompletionsRequest, OpenAiChatCompletionsResponse, OpenAiChatCompletionsResponse> {

    override val provider: Provider = Provider.OPENAI

    override suspend fun generate(request: OpenAiChatCompletionsRequest, map: TypedMap): OpenAiChatCompletionsResponse {
        val domainRequest = translator.toDomain(request)
        val domainResponse = service.generate(domainRequest)
        return translator.fromDomain(domainResponse)
    }

    override fun generateStream(request: OpenAiChatCompletionsRequest, map: TypedMap): Flow<OpenAiChatCompletionsResponse> {
        val domainRequest = translator.toDomain(request)
        return service.generateStream(domainRequest).map(translator::fromDomainEvent)
    }
}

