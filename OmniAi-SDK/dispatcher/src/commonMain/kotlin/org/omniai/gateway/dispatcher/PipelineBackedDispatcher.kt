package org.omniai.gateway.dispatcher

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.application.pipeline.GatewayContext
import org.omniai.sdk.application.pipeline.GatewayPipeline
import org.omniai.sdk.application.pipeline.RequestMode
import org.omniai.sdk.common.Either
import org.omniai.sdk.common.TypedMap
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent
import org.omniai.sdk.ports.inbound.DispatcherPort

class PipelineBackedDispatcher(
    private val pipeline: GatewayPipeline,
) : DispatcherPort {
    override suspend fun generate(
        request: CommonRequest,
        attributes: TypedMap,
    ): Either<DomainError, CommonResponse> {
        val context = GatewayContext(request = request, mode = RequestMode.UNARY, attributes = attributes)
        return pipeline.executeUnary(context)
    }

    override suspend fun generateStream(
        request: CommonRequest,
        attributes: TypedMap,
    ): Either<DomainError, Flow<CommonResponseEvent>> {
        val context = GatewayContext(request = request, mode = RequestMode.STREAM, attributes = attributes)
        return pipeline.executeStream(context)
    }
}
