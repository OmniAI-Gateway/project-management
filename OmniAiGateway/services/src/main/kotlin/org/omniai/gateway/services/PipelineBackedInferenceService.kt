package org.omniai.gateway.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.core.pipeline.GatewayPipeline
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

class PipelineBackedInferenceService(private val pipeline: GatewayPipeline) : InferenceServicePort {

    override suspend fun generate(request: CommonRequest): CommonResponse {
        val context = GatewayContext(request = request)
        val response = pipeline.executeUnary(context)
        return response
    }

    override fun generateStream(request: CommonRequest): Flow<CommonResponseEvent> = flow {
        val context = GatewayContext(request = request)
        val events = pipeline.executeStream(context)
        emitAll(events)
    }
}
