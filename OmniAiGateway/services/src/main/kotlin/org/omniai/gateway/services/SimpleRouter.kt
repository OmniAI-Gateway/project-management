package org.omniai.gateway.services

import org.omniai.sdk.core.ports.OutboundPort
import org.omniai.sdk.domain.requests.CommonRequest


import org.omniai.sdk.core.pipeline.InMemoryMetricsRegistry
import org.omniai.sdk.core.pipeline.ProviderModelKey
import kotlinx.coroutines.runBlocking

interface SimpleRouter {
    fun orderOutbounds(request: CommonRequest, outbounds: List<OutboundPort>): List<OutboundPort>
}

/**
 * Router that orders outbounds by lowest average latency (from metrics).
 */
class LatencyRouter : SimpleRouter {
    override fun orderOutbounds(request: CommonRequest, outbounds: List<OutboundPort>): List<OutboundPort> {
        return outbounds.sortedBy { outbound ->
            // Get metrics for this provider/model
            val key = ProviderModelKey(outbound.provider.value, outbound.model.model)
            val metrics = runBlocking {
                // Try to get metrics, fallback to high latency if not found
                try {
                    InMemoryMetricsRegistry
                        .let { registry ->
                            // Not public API for direct get, so simulate with recordSuccess (0 latency, no tokens)
                            registry.recordSuccess(key, 0, null)
                        }
                } catch (e: Exception) {
                    null
                }
            }
            metrics?.averageLatencyMs ?: Double.MAX_VALUE
        }
    }
}


