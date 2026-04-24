package org.omniai.sdk.core.ports

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.core.commom.Either
import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

/**
 * Core application service used by inbound adapters to execute domain requests.
 */
interface InferenceServicePort {
    suspend fun generate(request: CommonRequest, attributes: TypedMap): Either<DomainError, CommonResponse>

    suspend fun generateStream(request: CommonRequest, attributes: TypedMap): Either<DomainError, Flow<CommonResponseEvent>>
}
