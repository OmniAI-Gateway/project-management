package org.omniai.sdk.gateway.client.dsl.interceptors

import org.omniai.sdk.common.key
import org.omniai.sdk.common.AttributeKey
import org.omniai.sdk.application.pipeline.Interceptor
import org.omniai.sdk.ports.outbound.OutboundPort
import org.omniai.sdk.interceptors.circuitBreaker.CircuitBreakerConfig
import org.omniai.sdk.interceptors.circuitBreaker.CircuitBreakerInterceptor
import org.omniai.sdk.interceptors.circuitBreaker.CircuitBreakerStore
import org.omniai.sdk.interceptors.circuitBreaker.InMemoryCircuitBreakerStore
import org.omniai.sdk.interceptors.fallback.FallbackInterceptor

val DefaultDeniedOutboundsKey = key<Set<String>>("denied_outbounds")

class InterceptorsDsl {
    private val interceptors = mutableListOf<Interceptor>()
    private val deferredInterceptors = mutableListOf<(List<OutboundPort>) -> Interceptor>()

    fun use(interceptor: Interceptor) {
        interceptors += interceptor
    }

    fun use(deferred: (List<OutboundPort>) -> Interceptor) {
        deferredInterceptors += deferred
    }

    /**
     * Installs telemetry metrics interceptors based on configuration.
     */
    fun metrics(block: MetricsInterceptorBuilder.() -> Unit) {
        metricsInterceptorBuild(block).forEach(::use)
    }

    /**
     * Configures and installs a RateLimitInterceptor.
     */
    fun rateLimiting(block: RateLimitingInterceptorBuilder.() -> Unit) {
        use(RateLimitingInterceptorBuilder().apply(block).build())
    }

    /**
     * Configures and installs a CircuitBreakerInterceptor.
     */
    fun circuitBreaker(block: CircuitBreakerBuilder.() -> Unit = {}) {
        val builder = CircuitBreakerBuilder().apply(block)
        use { outbounds -> builder.build(outbounds) }
    }

    /**
     * Configures and installs a FallbackInterceptor.
     */
    fun fallback() {
        use { outbounds -> FallbackInterceptor(outbounds, DefaultDeniedOutboundsKey) }
    }

    internal fun build(outbounds: List<OutboundPort>): List<Interceptor> = 
        interceptors.toList() + deferredInterceptors.map { it(outbounds) }
}

class CircuitBreakerBuilder {
    var store: CircuitBreakerStore = InMemoryCircuitBreakerStore()
    var config: CircuitBreakerConfig = CircuitBreakerConfig()
    var deniedOutboundsKey: AttributeKey<Set<String>> = DefaultDeniedOutboundsKey

    internal fun build(outbounds: List<OutboundPort>): CircuitBreakerInterceptor {
        return CircuitBreakerInterceptor(store, config, deniedOutboundsKey, outbounds)
    }
}
