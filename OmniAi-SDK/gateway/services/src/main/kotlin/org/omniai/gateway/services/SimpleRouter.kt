package org.omniai.gateway.services

import kotlinx.coroutines.runBlocking
import org.omniai.sdk.core.pipeline.InMemoryMetricsRegistry
import org.omniai.sdk.core.pipeline.ProviderModelKey
import org.omniai.sdk.core.ports.OutboundPort
import org.omniai.sdk.domain.requests.CommonRequest

interface SimpleRouter {
    fun orderOutbounds(request: CommonRequest, outbounds: List<OutboundPort>): List<OutboundPort>
}

class LatencyRouter : SimpleRouter {
    override fun orderOutbounds(request: CommonRequest, outbounds: List<OutboundPort>): List<OutboundPort> {
        return outbounds.sortedBy { outbound ->
            val key = ProviderModelKey(outbound.provider.value, outbound.model.model)
            val metrics = runBlocking {
                try {
                    InMemoryMetricsRegistry.recordSuccess(key, 0, null)
                } catch (_: Exception) {
                    null
                }
            }
            metrics?.averageLatencyMs ?: Double.MAX_VALUE
        }
    }
}

