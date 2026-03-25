package org.omniaigateway.core.ports

import kotlinx.coroutines.flow.Flow
import org.omniaigateway.domain.common.Provider

/**
 * Provider-specific inbound contract exposed to client-facing adapters.
 */
interface InboundPort<in ClientReq, out ClientRes, out ClientEvent> {
    val provider: Provider

    suspend fun generate(request: ClientReq): ClientRes

    fun generateStream(request: ClientReq): Flow<ClientEvent>
}