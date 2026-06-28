package org.omniai.sdk.inbound.openai

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.common.Either
import org.omniai.sdk.common.Failure
import org.omniai.sdk.common.Success
import org.omniai.sdk.common.TypedMap
import org.omniai.sdk.common.failure
import org.omniai.sdk.common.success
import org.omniai.sdk.contracts.openai.input.OpenAiChatCompletionsRequest
import org.omniai.sdk.contracts.openai.output.OpenAiChatCompletionsResponse
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.ports.inbound.DispatcherPort
import org.omniai.sdk.ports.inbound.InboundPort

class OpenAiInboundAdapter(
    private val dispatcher: DispatcherPort,
    private val translator: OpenAiInboundTranslator = OpenAiInboundTranslator(),
) : InboundPort<OpenAiChatCompletionsRequest, OpenAiChatCompletionsResponse, OpenAiChatCompletionsResponse> {
    override val provider: Provider = Provider.OPENAI

    override suspend fun generate(
        request: OpenAiChatCompletionsRequest,
        map: TypedMap,
    ): Either<DomainError, OpenAiChatCompletionsResponse> {
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
        request: OpenAiChatCompletionsRequest,
        map: TypedMap,
    ): Either<DomainError, Flow<OpenAiChatCompletionsResponse>> {
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
