package org.omniai.sdk.core.pipeline

import org.omniai.sdk.core.ports.InferenceServicePort

internal class GatewayPipelineChain(
    private val interceptors: List<Interceptor>,
    private val service: InferenceServicePort,
    private val index: Int = 0
) : InterceptorChain {
    override suspend fun proceed(context: GatewayContext): PipelineResult {
        if (index >= interceptors.size) {
            return when (context.res) {
                is PipelineResult.Unary -> PipelineResult.Unary(service.generate(context.request))
                is PipelineResult.Stream -> PipelineResult.Stream(service.generateStream(context.request))
            }
        }

        val nextChain = GatewayPipelineChain(interceptors, service, index + 1)
        return interceptors[index].handle(context, nextChain)
    }
}

