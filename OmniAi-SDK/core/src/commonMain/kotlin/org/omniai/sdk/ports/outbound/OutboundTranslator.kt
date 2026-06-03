package org.omniai.sdk.ports.outbound

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

/**
 * Translates between the core domain models and provider-specific DTOs for outbound requests.
 * Implement this interface in your outbound adapters (e.g., OpenAIAdapter, GeminiAdapter).
 *
 * @param ProviderReq The provider-specific request payload (e.g., OpenAiChatRequestDto).
 * @param ProviderRes The provider-specific response payload (e.g., OpenAiChatResponseDto).
 * @param ProviderEvent The provider-specific streaming event payload (e.g., OpenAiChunkDto).
 */
interface OutboundTranslator<out ProviderReq, in ProviderRes, in ProviderEvent> {

    /**
     * Converts a core domain request into a provider-specific request payload.
     */
    fun fromDomain(domainRequest: CommonRequest): ProviderReq

    /**
     * Converts a provider-specific response payload back into the core domain model.
     */
    fun toDomain(providerResponse: ProviderRes): CommonResponse

    /**
     * Converts a provider-specific streaming event into the core domain event model.
     */
    fun toDomainEvent(providerEvent: Flow<ProviderEvent>): Flow<CommonResponseEvent>
}