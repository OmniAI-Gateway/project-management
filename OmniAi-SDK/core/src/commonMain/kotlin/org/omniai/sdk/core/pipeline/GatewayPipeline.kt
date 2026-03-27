package org.omniai.sdk.core.pipeline

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

class GatewayPipeline internal constructor(
    private val interceptors: List<Interceptor>,
    private val service: InferenceServicePort
) {
    private suspend fun executeRaw(context: GatewayContext): PipelineResult {
        return GatewayPipelineChain(interceptors, service, 0).proceed(context)
    }

    suspend fun executeUnary(context: GatewayContext): CommonResponse {
        return executeRaw(context).requireUnaryResponse()
    }

    suspend fun executeStream(context: GatewayContext): Flow<CommonResponseEvent> {
        return executeRaw(context).requireStreamEvents()
    }
}

