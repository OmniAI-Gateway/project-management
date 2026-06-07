package org.omniai.sdk.interceptors.circuitBreaker

import org.omniai.sdk.common.AttributeKey
import org.omniai.sdk.application.pipeline.GatewayContext
import org.omniai.sdk.application.pipeline.Interceptor
import org.omniai.sdk.application.pipeline.InterceptorChain
import org.omniai.sdk.application.pipeline.PipelineResult
import org.omniai.sdk.ports.outbound.OutboundPort
import org.omniai.sdk.domain.errors.ApiDownError
import org.omniai.sdk.interceptors.metrics.MetricsPort

class CircuitBreakerInterceptor(
    private val store: CircuitBreakerStore,
    private val config: CircuitBreakerConfig,
    private val deniedOutboundsKey: AttributeKey<Set<String>>,
    private val outbounds: List<OutboundPort>,
    private val metricsPort: MetricsPort? = null
) : Interceptor {

    private val transitionCounter = metricsPort?.counter(
        "gateway.circuitbreaker.transitions",
        "Transições de estado do circuit breaker",
        "1"
    )

    private val rejectedCounter = metricsPort?.counter(
        "gateway.circuitbreaker.rejected",
        "Requests rejeitados por circuit breaker OPEN",
        "1"
    )

    override suspend fun handle(context: GatewayContext, chain: InterceptorChain): PipelineResult {
        // Resolve target outbound based on request
        val targetOutbound = outbounds.find { it.provider.value == context.request.provider.value && it.model.model == context.request.model }
            ?: return chain.proceed(context) // Let dispatcher handle the missing outbound error

        val outboundId = targetOutbound.key
        val currentState = store.getState(outboundId)

        if (currentState == CircuitState.OPEN) {
            // Update denied outbounds in context
            val attributes = context.attributes
            val denied = (attributes[deniedOutboundsKey] ?: emptySet()).toMutableSet()
            denied.add(outboundId)
            attributes[deniedOutboundsKey] = denied

            rejectedCounter?.add(1.0, mapOf(
                "outbound" to outboundId,
                "provider" to context.request.provider.value,
                "model" to context.request.model
            ))

            // Fast fail
            val error = ApiDownError("Circuit breaker is OPEN for outbound: $outboundId")
            return PipelineResult.Error(error)
        }

        val result = chain.proceed(context)

        // Record metrics
        when (result) {
            is PipelineResult.Error -> {
                store.recordFailure(outboundId)
                val failures = store.getFailures(outboundId)
                if (failures >= config.failureThreshold && currentState != CircuitState.OPEN) {
                    store.transitionState(outboundId, CircuitState.OPEN)
                    transitionCounter?.add(1.0, mapOf(
                        "outbound" to outboundId,
                        "from" to currentState.name,
                        "to" to CircuitState.OPEN.name
                    ))
                }
            }
            is PipelineResult.Unary, is PipelineResult.Stream -> {
                store.recordSuccess(outboundId)
                if (currentState == CircuitState.HALF_OPEN) {
                    store.transitionState(outboundId, CircuitState.CLOSED)
                    transitionCounter?.add(1.0, mapOf(
                        "outbound" to outboundId,
                        "from" to CircuitState.HALF_OPEN.name,
                        "to" to CircuitState.CLOSED.name
                    ))
                }
            }
            is PipelineResult.NoResult -> {}
        }

        return result
    }
}

