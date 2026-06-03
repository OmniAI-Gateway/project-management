package org.omniai.sdk.ports.inbound

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

/**
 * Translates between client-specific inbound payloads and the core domain models.
 * Implement this interface in your API controllers or entry points.
 *
 * @param ClientReq The inbound request payload received from the client.
 * @param ClientRes The outbound response payload returned to the client.
 * @param ClientEvent The outbound streaming event payload returned to the client.
 */
interface InboundTranslator<in ClientReq, out ClientRes, out ClientEvent> {
    /**
     * Identifies the provider format this translator handles (e.g., OpenAI format).
     */
    val provider: Provider

    /**
     * Converts an external client request payload into the core domain model.
     */
    fun toDomain(clientRequest: ClientReq): CommonRequest

    /**
     * Converts a core domain response into the client-specific response format.
     */
    fun fromDomain(domainResponse: CommonResponse): ClientRes

    /**
     * Converts a core domain streaming event into the client-specific event format.
     */
    fun fromDomainEvent(domainEvent: Flow<CommonResponseEvent>): Flow<ClientEvent>
}
