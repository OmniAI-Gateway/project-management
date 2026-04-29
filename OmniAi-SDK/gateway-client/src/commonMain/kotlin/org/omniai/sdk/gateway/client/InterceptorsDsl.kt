package org.omniai.sdk.gateway.client

import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.domain.common.Provider

class InterceptorsDsl {
    private val globalInterceptors = mutableListOf<Interceptor>()
    private val localInterceptors = mutableMapOf<Provider, MutableList<Interceptor>>()

    fun global(interceptor: Interceptor) {
        globalInterceptors += interceptor
    }

    fun local(provider: Provider, interceptor: Interceptor) {
        localInterceptors.getOrPut(provider) { mutableListOf() }.add(interceptor)
    }

    /**
     * Installs telemetry interceptors in global scope.
     * If [TelemetryMetricsInterceptorBuilder.tracer] is set, tracing runs before metrics.
     */
    fun telemetryMetrics(block: TelemetryMetricsInterceptorBuilder.() -> Unit) {
        telemetryMetricsInterceptorBuild(block).forEach(::global)
    }

    internal fun build(): InterceptorRegistration = InterceptorRegistration(
        global = globalInterceptors.toList(),
        localByProvider = localInterceptors.mapValues { (_, list) -> list.toList() }
    )
}

