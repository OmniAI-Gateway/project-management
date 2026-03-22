package org.omniaigateway.core.ports

import kotlinx.coroutines.flow.Flow
import org.omniaigateway.domain.common.Model
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.requests.CommonRequest
import org.omniaigateway.domain.responses.CommonResponse
import org.omniaigateway.domain.responses.CommonResponseEvent

/**
 * Provider strategy for executing outbound calls from the core request model.
 */
interface OutboundAdapter {
    val provider: Provider

    val model: Model

    suspend fun execute(request: CommonRequest): CommonResponse

    fun generate(request: CommonRequest): Flow<CommonResponseEvent>
}

