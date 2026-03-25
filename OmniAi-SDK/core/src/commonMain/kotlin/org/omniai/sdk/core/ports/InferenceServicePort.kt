package org.omniai.sdk.core.ports

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

/**
 * Core application service used by inbound adapters to execute domain requests.
 */
interface InferenceServicePort {
    suspend fun generate(request: CommonRequest): CommonResponse

    fun generateStream(request: CommonRequest): Flow<CommonResponseEvent>
}

