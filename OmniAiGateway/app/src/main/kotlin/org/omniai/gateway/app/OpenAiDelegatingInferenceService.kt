package org.omniai.gateway.app

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.adapters.openai.OpenAiOutboundAdapter
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

/**
 * Minimal bridge service that forwards domain requests to the OpenAI outbound adapter.
 */
class OpenAiDelegatingInferenceService(
    private val openAiOutboundAdapter: OpenAiOutboundAdapter
) : InferenceServicePort {

    override suspend fun generate(request: CommonRequest): CommonResponse =
        openAiOutboundAdapter.generate(request)

    override fun generateStream(request: CommonRequest): Flow<CommonResponseEvent> =
        openAiOutboundAdapter.generateStream(request)
}

