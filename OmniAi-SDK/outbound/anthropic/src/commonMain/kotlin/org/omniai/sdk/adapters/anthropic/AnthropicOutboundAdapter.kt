package org.omniai.sdk.adapters.anthropic

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.core.ports.OutboundPort
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

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