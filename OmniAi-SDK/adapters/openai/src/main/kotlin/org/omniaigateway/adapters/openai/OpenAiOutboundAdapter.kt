package org.omniaigateway.adapters.openai

import org.omniaigateway.core.ports.OutboundAdapter
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.requests.CommonRequest
import org.omniaigateway.domain.responses.CommonResponse

class OpenAiOutboundAdapter : OutboundAdapter {
    override val provider: Provider = Provider.OPENAI

    override suspend fun execute(request: CommonRequest): CommonResponse {
        throw UnsupportedOperationException("Wire this adapter to a concrete HTTP client module")
    }
}

