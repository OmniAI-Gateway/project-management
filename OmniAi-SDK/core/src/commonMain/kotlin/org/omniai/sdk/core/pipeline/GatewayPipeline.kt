package org.omniai.sdk.core.pipeline

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.core.commom.Either
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

class GatewayPipeline internal constructor(
    private val interceptors: List<Interceptor>,
    private val service: InferenceServicePort
) {
    private suspend fun executeRaw(context: GatewayContext): PipelineResult {
        return GatewayPipelineChain(interceptors, service, 0).proceed(context)
    }

    suspend fun executeUnary(context: GatewayContext): Either<DomainError, CommonResponse> {
        return executeRaw(context).requireUnaryResponse()
    }

    suspend fun executeStream(context: GatewayContext): Either<DomainError, Flow<CommonResponseEvent>> {
        return executeRaw(context).requireStreamEvents()
    }
}
