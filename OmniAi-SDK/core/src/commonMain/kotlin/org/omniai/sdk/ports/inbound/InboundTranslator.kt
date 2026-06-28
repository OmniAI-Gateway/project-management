package org.omniai.sdk.ports.inbound

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

interface InboundTranslator<in ClientReq, out ClientRes, out ClientEvent> {
    val provider: Provider

    fun toDomain(clientRequest: ClientReq): CommonRequest

    fun fromDomain(domainResponse: CommonResponse): ClientRes

    fun fromDomainEvent(domainEvent: Flow<CommonResponseEvent>): Flow<ClientEvent>
}
