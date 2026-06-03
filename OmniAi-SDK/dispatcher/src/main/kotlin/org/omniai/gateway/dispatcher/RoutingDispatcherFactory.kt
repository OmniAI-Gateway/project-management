package org.omniai.gateway.dispatcher

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.common.Either
import org.omniai.sdk.common.TypedMap
import org.omniai.sdk.common.failure
import org.omniai.sdk.ports.inbound.DispatcherPort
import org.omniai.sdk.ports.outbound.OutboundPort
import org.omniai.sdk.ports.inbound.DispatcherAdapter
import org.omniai.sdk.ports.inbound.dispatcherAdapter
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.errors.UnknownDomainError
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

fun routingDispatcherFactory(outbounds: List<OutboundPort>): DispatcherPort {
    return dispatcherAdapter {
        unary { request: CommonRequest, attributes: TypedMap ->
            val outbound = outbounds.find { it.provider.value == request.provider.value && it.model.model == request.model }
            outbound?.generate(request.withAttributes(attributes))
                ?: failure(UnknownDomainError(message = "No outbound available for provider=${request.provider.value} model=${request.model}"))
        }
        stream { request: CommonRequest, attributes: TypedMap ->
            val outbound = outbounds.find { it.provider.value == request.provider.value && it.model.model == request.model }
            outbound?.generateStream(request.withAttributes(attributes))
                ?: failure(UnknownDomainError(message = "No outbound available for provider=${request.provider.value} model=${request.model}"))
        }
    }
}

private fun CommonRequest.withAttributes(attributes: TypedMap): CommonRequest {
    if (attributes.isEmpty()) return this
    val merged = providerOptions.copy().also { it.putAll(attributes) }
    return copy(providerOptions = merged)
}