package org.omniai.sdk.ports.outbound

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.common.Either
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

interface OutboundPort {
    val provider: Provider
    val model: Model
    val key: String

    suspend fun generate(request: CommonRequest): Either<DomainError, CommonResponse>

    suspend fun generateStream(request: CommonRequest): Either<DomainError, Flow<CommonResponseEvent>>
}
