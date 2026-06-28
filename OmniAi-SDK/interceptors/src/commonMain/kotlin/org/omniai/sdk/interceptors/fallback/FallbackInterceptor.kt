package org.omniai.sdk.interceptors.fallback

import org.omniai.sdk.application.pipeline.GatewayContext
import org.omniai.sdk.application.pipeline.Interceptor
import org.omniai.sdk.application.pipeline.InterceptorChain
import org.omniai.sdk.application.pipeline.PipelineResult
import org.omniai.sdk.common.AttributeKey
import org.omniai.sdk.interceptors.metrics.MetricsPort
import org.omniai.sdk.ports.outbound.OutboundPort

class FallbackInterceptor(
    private val outbounds: List<OutboundPort>,
    private val deniedOutboundsKey: AttributeKey<Set<String>>,
    private val metricsPort: MetricsPort? = null,
) : Interceptor {
    private val fallbackCounter =
        metricsPort?.counter(
            "gateway.fallback.activations",
            "Vezes que o fallback ativou um provider alternativo",
            "1",
        )

    override suspend fun handle(
        context: GatewayContext,
        chain: InterceptorChain,
    ): PipelineResult {
        var lastResult: PipelineResult? = null
        val attributes = context.attributes

        val initiallyDenied = attributes[deniedOutboundsKey] ?: emptySet()
        val denied = initiallyDenied.toMutableSet()

        val primary =
            outbounds.find { it.provider.value == context.request.provider.value && it.model.model == context.request.model }

        val alternatives = outbounds.filterNot { it === primary }

        val sequenceToTry = listOfNotNull(primary) + alternatives

        var isPrimaryFailed = false

        for (outbound in sequenceToTry) {
            if (denied.contains(outbound.key)) {
                continue
            }

            val newRequest =
                context.request.copy(
                    provider = outbound.provider,
                    model = outbound.model.model,
                )

            attributes[deniedOutboundsKey] = denied.toSet()

            val newContext =
                GatewayContext(
                    request = newRequest,
                    attributes = attributes,
                    mode = context.mode,
                    res = context.res,
                )

            when (val result = chain.proceed(newContext)) {
                is PipelineResult.Unary -> {
                    if (isPrimaryFailed) {
                        fallbackCounter?.add(
                            1.0,
                            mapOf(
                                "original.provider" to context.request.provider.value,
                                "original.model" to context.request.model,
                                "fallback.provider" to outbound.provider.value,
                                "fallback.model" to outbound.model.model,
                            ),
                        )
                    }
                    return result
                }

                is PipelineResult.Stream -> {
                    if (isPrimaryFailed) {
                        fallbackCounter?.add(
                            1.0,
                            mapOf(
                                "original.provider" to context.request.provider.value,
                                "original.model" to context.request.model,
                                "fallback.provider" to outbound.provider.value,
                                "fallback.model" to outbound.model.model,
                            ),
                        )
                    }
                    return result
                }

                is PipelineResult.NoResult -> {
                    return result
                }

                is PipelineResult.Error -> {
                    lastResult = result
                    denied.add(outbound.key)
                    if (outbound === primary) {
                        isPrimaryFailed = true
                    }
                }
            }
        }

        return lastResult ?: PipelineResult.NoResult
    }
}
