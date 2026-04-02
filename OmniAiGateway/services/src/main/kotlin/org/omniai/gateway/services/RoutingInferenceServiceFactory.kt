package org.omniai.gateway.services

import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.core.ports.OutboundPort
import org.omniai.sdk.core.ports.serviceAdapter
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.requests.CommonRequest

object RoutingInferenceServiceFactory {
    fun create(
        outbounds: List<OutboundPort>,
        fallbackProvider: Provider? = null
    ): InferenceServicePort {
        val outboundsByProvider = outbounds.groupBy { it.provider }

        return serviceAdapter {
            unary { request ->
                val outbound = resolveOutbound(request, outboundsByProvider, fallbackProvider)
                outbound.generate(request)
            }
            stream { request ->
                val outbound = resolveOutbound(request, outboundsByProvider, fallbackProvider)
                outbound.generateStream(request)
            }
        }
    }
}

private fun resolveOutbound(
    request: CommonRequest,
    outboundsByProvider: Map<Provider, List<OutboundPort>>,
    fallbackProvider: Provider?
): OutboundPort {
    val providerCandidates = outboundsByProvider[request.provider].orEmpty()
    val selectedFromProvider = providerCandidates.firstOrNull { it.model.model == request.model } ?: providerCandidates.firstOrNull()
    if (selectedFromProvider != null) return selectedFromProvider

    if (fallbackProvider != null) {
        val fallbackCandidates = outboundsByProvider[fallbackProvider].orEmpty()
        val selectedFallback = fallbackCandidates.firstOrNull { it.model.model == request.model } ?: fallbackCandidates.firstOrNull()
        if (selectedFallback != null) return selectedFallback
    }

    error("No outbound configured for provider=${request.provider.value} model=${request.model}")
}

