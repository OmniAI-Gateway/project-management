package org.omniai.sdk.core.pipeline

import org.omniai.sdk.core.commom.Either
import org.omniai.sdk.core.ports.DispatcherPort

internal class GatewayPipelineChain(
    private val interceptors: List<Interceptor>,
    private val dispatcher: DispatcherPort,
    private val index: Int = 0
) : InterceptorChain {
    override suspend fun proceed(context: GatewayContext): PipelineResult {
        if (index >= interceptors.size) {
            return when (context.res) {
                is PipelineResult.Unary -> when (val result = dispatcher.generate(context.request,context.attributes)) {
                    is Either.Right -> PipelineResult.Unary(result.value)
                    is Either.Left -> PipelineResult.Error(result.value)
                }

                is PipelineResult.Stream -> when (val result = dispatcher.generateStream(context.request, context.attributes)) {
                    is Either.Right -> PipelineResult.Stream(result.value)
                    is Either.Left -> PipelineResult.Error(result.value)
                }

                is PipelineResult.Error -> context.res
                // Antes devolvia NoResult e quebrava o contrato do executeUnary/executeStream.
                // Agora escolhe a chamada final com base no modo pedido no GatewayContext.
                is PipelineResult.NoResult -> when (context.mode) {
                    RequestMode.UNARY -> when (val result = dispatcher.generate(context.request, context.attributes)) {
                        is Either.Right -> PipelineResult.Unary(result.value)
                        is Either.Left -> PipelineResult.Error(result.value)
                    }

                    RequestMode.STREAM -> when (val result = dispatcher.generateStream(context.request, context.attributes)) {
                        is Either.Right -> PipelineResult.Stream(result.value)
                        is Either.Left -> PipelineResult.Error(result.value)
                    }
                }
            }
        }

        val nextChain = GatewayPipelineChain(interceptors, dispatcher, index + 1)
        return interceptors[index].handle(context, nextChain)
    }
}
