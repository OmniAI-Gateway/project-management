package org.omniaigateway.core.ports

import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.requests.CommonRequest
import org.omniaigateway.domain.responses.CommonResponse

/**
 * Provider strategy for executing outbound calls from the core request model.
 */
interface OutboundAdapter {
    val provider: Provider

    fun supports(request: CommonRequest): Boolean = request.provider == provider

    suspend fun execute(request: CommonRequest): CommonResponse
}

