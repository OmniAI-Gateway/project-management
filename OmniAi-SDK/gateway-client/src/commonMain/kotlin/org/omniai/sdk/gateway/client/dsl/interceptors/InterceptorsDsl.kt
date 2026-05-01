package org.omniai.sdk.gateway.client.dsl.interceptors

import org.omniai.sdk.core.pipeline.Interceptor

class InterceptorsDsl {
    private val interceptors = mutableListOf<Interceptor>()

    fun use(interceptor: Interceptor) {
        interceptors += interceptor
    }

    /**
     * Installs telemetry metrics interceptors based on configuration.
     */
    fun telemetryMetrics(block: TelemetryMetricsInterceptorBuilder.() -> Unit) {
        telemetryMetricsInterceptorBuild(block).forEach(::use)
    }

    internal fun build(): List<Interceptor> = interceptors.toList()
}
