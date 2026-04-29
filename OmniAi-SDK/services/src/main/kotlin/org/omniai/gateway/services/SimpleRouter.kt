package org.omniai.gateway.services

import org.omniai.sdk.core.ports.OutboundPort
import org.omniai.sdk.domain.requests.CommonRequest

interface SimpleRouter {
    fun orderOutbounds(request: CommonRequest, outbounds: List<OutboundPort>): List<OutboundPort>
}

class FallbackRouter : SimpleRouter {
    override fun orderOutbounds(request: CommonRequest, outbounds: List<OutboundPort>): List<OutboundPort> {
        if (outbounds.isEmpty()) return emptyList()

        val primaryOutbound = outbounds.find {
            it.provider.value == request.provider.value && it.model.model == request.model
        }
        if (primaryOutbound == null) {
            return outbounds
        }
        val fallbacks = outbounds.filterNot { it === primaryOutbound }
        return listOf(primaryOutbound) + fallbacks
    }
}