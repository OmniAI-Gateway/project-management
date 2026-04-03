package org.omniai.sdk.core.pipeline

import org.omniai.sdk.core.commom.Either
import org.omniai.sdk.core.ports.InferenceServicePort

internal class GatewayPipelineChain(
    private val interceptors: List<Interceptor>,
    private val service: InferenceServicePort,
    private val index: Int = 0
) : InterceptorChain {
    override suspend fun proceed(context: GatewayContext): PipelineResult {
        if (index >= interceptors.size) {
            return when (context.res) {
                is PipelineResult.Unary -> when (val result = service.generate(context.request)) {
                    is Either.Right -> PipelineResult.Unary(result.value)
                    is Either.Left -> PipelineResult.Error(result.value)
                }

                is PipelineResult.Stream -> when (val result = service.generateStream(context.request)) {
                    is Either.Right -> PipelineResult.Stream(result.value)
                    is Either.Left -> PipelineResult.Error(result.value)
                }

                is PipelineResult.Error -> context.res
                is PipelineResult.NoResult -> PipelineResult.NoResult
            }
        }

        val nextChain = GatewayPipelineChain(interceptors, service, index + 1)
        return interceptors[index].handle(context, nextChain)
    }
}
