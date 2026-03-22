package org.omniaigateway.core.ports

import org.omniaigateway.domain.requests.CommonRequest
import org.omniaigateway.domain.responses.CommonResponse
import org.omniaigateway.domain.responses.CommonResponseEvent

/**
 * Translates between the core domain models and provider-specific DTOs for outbound requests.
 * Implement this interface in your outbound adapters (e.g., OpenAIAdapter, GeminiAdapter).
 *
 * @param ProviderReq The provider-specific request payload (e.g., OpenAiChatRequestDto).
 * @param ProviderRes The provider-specific response payload (e.g., OpenAiChatResponseDto).
 * @param ProviderEvent The provider-specific streaming event payload (e.g., OpenAiChunkDto).
 */
interface AdapterTranslator<out ProviderReq, in ProviderRes, in ProviderEvent> {

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
    fun toDomainEvent(providerEvent: ProviderEvent): CommonResponseEvent
}
