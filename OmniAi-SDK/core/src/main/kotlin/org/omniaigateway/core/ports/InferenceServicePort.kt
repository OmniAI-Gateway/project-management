package org.omniaigateway.core.ports

import kotlinx.coroutines.flow.Flow
import org.omniaigateway.domain.requests.CommonRequest
import org.omniaigateway.domain.responses.CommonResponse
import org.omniaigateway.domain.responses.CommonResponseEvent

/**
 * Core application service used by inbound adapters to execute domain requests.
 */
interface InferenceServicePort {
    suspend fun generate(request: CommonRequest): CommonResponse

    fun generateStream(request: CommonRequest): Flow<CommonResponseEvent>
}

