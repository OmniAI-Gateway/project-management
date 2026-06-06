package org.omniai.sdk.ports.outbound

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

interface OutboundTranslator<out ProviderReq, in ProviderRes, in ProviderEvent> {
    fun fromDomain(domainRequest: CommonRequest): ProviderReq
    fun toDomain(providerResponse: ProviderRes): CommonResponse
    fun toDomainEvent(providerEvent: Flow<ProviderEvent>): Flow<CommonResponseEvent>
}