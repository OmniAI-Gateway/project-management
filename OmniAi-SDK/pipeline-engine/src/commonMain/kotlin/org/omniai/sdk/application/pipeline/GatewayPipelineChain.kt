package org.omniai.sdk.application.pipeline

import org.omniai.sdk.common.Either
import org.omniai.sdk.ports.inbound.DispatcherPort

internal class GatewayPipelineChain(
    private val interceptors: List<Interceptor>,
    private val dispatcher: DispatcherPort,
    private val index: Int = 0,
) : InterceptorChain {
    override suspend fun proceed(context: GatewayContext): PipelineResult {
        if (index >= interceptors.size) {
            return when (context.res) {
                is PipelineResult.Unary -> {
                    when (
                        val result =
                            dispatcher.generate(context.request, context.attributes)
                    ) {
                        is Either.Right -> PipelineResult.Unary(result.value)
                        is Either.Left -> PipelineResult.Error(result.value)
                    }
                }

                is PipelineResult.Stream -> {
                    when (
                        val result =
                            dispatcher.generateStream(context.request, context.attributes)
                    ) {
                        is Either.Right -> PipelineResult.Stream(result.value)
                        is Either.Left -> PipelineResult.Error(result.value)
                    }
                }

                is PipelineResult.Error -> {
                    context.res
                }

                is PipelineResult.NoResult -> {
                    when (context.mode) {
                        RequestMode.UNARY -> {
                            when (val result = dispatcher.generate(context.request, context.attributes)) {
                                is Either.Right -> PipelineResult.Unary(result.value)
                                is Either.Left -> PipelineResult.Error(result.value)
                            }
                        }

                        RequestMode.STREAM -> {
                            when (
                                val result =
                                    dispatcher.generateStream(context.request, context.attributes)
                            ) {
                                is Either.Right -> PipelineResult.Stream(result.value)
                                is Either.Left -> PipelineResult.Error(result.value)
                            }
                        }
                    }
                }
            }
        }

        val nextChain = GatewayPipelineChain(interceptors, dispatcher, index + 1)
        return interceptors[index].handle(context, nextChain)
    }
}
