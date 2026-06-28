package org.omniai.sdk.ports.inbound

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.common.Either
import org.omniai.sdk.common.TypedMap
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.errors.DomainError

interface InboundPort<in ClientReq, out ClientRes, out ClientEvent> {
    val provider: Provider

    suspend fun generate(
        request: ClientReq,
        map: TypedMap,
    ): Either<DomainError, ClientRes>

    suspend fun generateStream(
        request: ClientReq,
        map: TypedMap,
    ): Either<DomainError, Flow<ClientEvent>>
}
