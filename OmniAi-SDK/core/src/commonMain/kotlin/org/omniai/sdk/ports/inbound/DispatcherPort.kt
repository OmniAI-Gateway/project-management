package org.omniai.sdk.ports.inbound

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.common.Either
import org.omniai.sdk.common.TypedMap
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

interface DispatcherPort {
    suspend fun generate(request: CommonRequest, attributes: TypedMap): Either<DomainError, CommonResponse>
    suspend fun generateStream(request: CommonRequest, attributes: TypedMap): Either<DomainError, Flow<CommonResponseEvent>>
}
