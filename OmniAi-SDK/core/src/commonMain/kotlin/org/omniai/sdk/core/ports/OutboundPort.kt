package org.omniai.sdk.core.ports

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

/**
 * Provider strategy for executing outbound calls from the core request model.
 */
interface OutboundPort {
    val provider: Provider

    val model: Model

    suspend fun generate(request: CommonRequest): CommonResponse

    fun generateStream(request: CommonRequest): Flow<CommonResponseEvent>
}

