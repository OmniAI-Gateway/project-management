package org.omniai.gateway.dispatcher

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.core.commom.Either
import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.core.commom.failure
import org.omniai.sdk.core.ports.DispatcherPort
import org.omniai.sdk.core.ports.OutboundPort
import org.omniai.sdk.core.ports.dispatcherAdapter
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.errors.UnknownDomainError
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

fun routingDispatcherFactory(outbounds: List<OutboundPort>): DispatcherPort {
    return dispatcherAdapter {
        unary { request: CommonRequest, attributes: TypedMap ->
            val outbound = outbounds.find { it.provider.value == request.provider.value && it.model.model == request.model }
            if (outbound != null) {
                outbound.generate(request.withAttributes(attributes))
            } else {
                failure(UnknownDomainError(message = "No outbound available for provider=${request.provider.value} model=${request.model}"))
            }
        }
        stream { request: CommonRequest, attributes: TypedMap ->
            val outbound = outbounds.find { it.provider.value == request.provider.value && it.model.model == request.model }
            if (outbound != null) {
                outbound.generateStream(request.withAttributes(attributes))
            } else {
                failure(UnknownDomainError(message = "No outbound available for provider=${request.provider.value} model=${request.model}"))
            }
        }
    }
}

private fun CommonRequest.withAttributes(attributes: TypedMap): CommonRequest {
    if (attributes.isEmpty()) return this
    val merged = providerOptions.copy().also { it.putAll(attributes) }
    return copy(providerOptions = merged)
}