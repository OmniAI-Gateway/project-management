package org.omniai.sdk.inbound.openai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.omniai.sdk.contracts.openai.input.OpenAiChatCompletionsRequest
import org.omniai.sdk.contracts.openai.output.OpenAiChatCompletionsResponse
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

class OpenAiInboundAdapter(
    private val service: InferenceServicePort,
    private val translator: OpenAiInboundTranslator = OpenAiInboundTranslator()
) : InboundPort<OpenAiChatCompletionsRequest, OpenAiChatCompletionsResponse, OpenAiChatCompletionsResponse> {

    override val provider: Provider = Provider.OPENAI

    override suspend fun generate(
        request: OpenAiChatCompletionsRequest,
        map: TypedMap,
    ): Either<DomainError, OpenAiChatCompletionsResponse> {
        val domainRequest = translator.toDomain(request)
        return when (val domainResponse = service.generate(domainRequest)) {
            is Success -> success(translator.fromDomain(domainResponse.value))
            is Failure -> failure(domainResponse.value)
        }
    }

    override suspend fun generateStream(
        request: OpenAiChatCompletionsRequest,
        map: TypedMap,
    ): Either<DomainError, Flow<OpenAiChatCompletionsResponse>> {
        val domainRequest = translator.toDomain(request)
        return when (val streamResult = service.generateStream(domainRequest)) {
            is Success -> success(streamResult.value.map(translator::fromDomainEvent))
            is Failure -> failure(streamResult.value)
        }
    }
}
