package org.omniai.gateway.app

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.core.pipeline.GatewayPipeline
import org.omniai.sdk.core.pipeline.MetricsSnapshotKey
import org.omniai.sdk.core.pipeline.PipelineResult
import org.omniai.sdk.core.pipeline.ProviderModelMetrics
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

/**
 * InferenceServicePort wrapper that runs requests through the SDK pipeline.
 */
class PipelineBackedInferenceService(
    private val pipeline: GatewayPipeline,
    private val onMetricsCaptured: (CommonRequest, ProviderModelMetrics) -> Unit = { _, _ -> }
) : InferenceServicePort {

    override suspend fun generate(request: CommonRequest): CommonResponse {
        val context = GatewayContext(request = request, res = unaryMarker(request))
        val response = pipeline.executeUnary(context)
        context.attributes[MetricsSnapshotKey]?.let { onMetricsCaptured(request, it) }
        return response
    }

    override fun generateStream(request: CommonRequest): Flow<CommonResponseEvent> = flow {
        val context = GatewayContext(request = request, res = PipelineResult.Stream(emptyFlow()))
        val events = pipeline.executeStream(context)
        emitAll(
            events.onCompletion {
                context.attributes[MetricsSnapshotKey]?.let { onMetricsCaptured(request, it) }
            }
        )
    }
}

private fun unaryMarker(request: CommonRequest): PipelineResult.Unary =
    PipelineResult.Unary(
        CommonResponse(
            provider = request.provider,
            model = request.model,
            choices = emptyList()
        )
    )


