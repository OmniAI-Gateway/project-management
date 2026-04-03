package org.omniai.sdk.core.ports

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.core.commom.Either
import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.errors.DomainError

/**
 * Provider-specific inbound contract exposed to client-facing adapters.
 */
interface InboundPort<in ClientReq, out ClientRes, out ClientEvent> {
    val provider: Provider

    suspend fun generate(request: ClientReq, map: TypedMap): Either<DomainError, ClientRes>

    suspend fun generateStream(request: ClientReq, map: TypedMap): Either<DomainError, Flow<ClientEvent>>
}