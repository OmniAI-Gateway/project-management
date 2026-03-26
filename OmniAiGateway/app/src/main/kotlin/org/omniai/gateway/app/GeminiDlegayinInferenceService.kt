package org.omniai.gateway.app

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.adapters.gemini.GeminiOutboundAdapter
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

class GeminiDlegayinInferenceService(private val geminiOutboundAdapter: GeminiOutboundAdapter) : InferenceServicePort {
        override suspend fun generate(request: CommonRequest): CommonResponse =
            geminiOutboundAdapter.generate(request)

        override fun generateStream(request: CommonRequest): Flow<CommonResponseEvent> =
            geminiOutboundAdapter.generateStream(request)
    }
