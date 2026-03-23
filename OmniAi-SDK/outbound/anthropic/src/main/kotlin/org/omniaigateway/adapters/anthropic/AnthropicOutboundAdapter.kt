package org.omniaigateway.adapters.anthropic

import kotlinx.coroutines.flow.Flow
import org.omniaigateway.core.ports.OutboundPort
import org.omniaigateway.domain.common.Model
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.requests.CommonRequest
import org.omniaigateway.domain.responses.CommonResponse
import org.omniaigateway.domain.responses.CommonResponseEvent

class AnthropicOutboundAdapter: OutboundPort {
    override val provider: Provider
        get() = TODO("Not yet implemented")
    override val model: Model
        get() = TODO("Not yet implemented")

    override suspend fun generate(request: CommonRequest): CommonResponse {
        TODO("Not yet implemented")
    }

    override fun generateStream(request: CommonRequest): Flow<CommonResponseEvent> {
        TODO("Not yet implemented")
    }
}