package org.omniai.gateway.services

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.core.commom.Either
import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.core.pipeline.GatewayPipeline
import org.omniai.sdk.core.pipeline.RequestMode
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

class PipelineBackedInferenceService(private val pipeline: GatewayPipeline) : InferenceServicePort {

    override suspend fun generate(request: CommonRequest, attributes: TypedMap): Either<DomainError, CommonResponse> {
        val context = GatewayContext(request = request, mode = RequestMode.UNARY, attributes = attributes)
        return pipeline.executeUnary(context)
    }

    override suspend fun generateStream(request: CommonRequest,attributes: TypedMap): Either<DomainError, Flow<CommonResponseEvent>> {
        val context = GatewayContext(request = request, mode = RequestMode.STREAM, attributes =  attributes)
        return pipeline.executeStream(context)
    }
}

