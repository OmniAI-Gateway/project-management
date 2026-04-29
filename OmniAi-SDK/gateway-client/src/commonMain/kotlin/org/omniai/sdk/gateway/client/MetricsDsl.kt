package org.omniai.sdk.gateway.client

import org.omniai.sdk.core.pipeline.Interceptor

class MetricsDsl {
    var enabled: Boolean = true
    private val metrics = mutableSetOf(
        GatewayMetric.REQUEST_COUNT,
        GatewayMetric.ERROR_COUNT,
        GatewayMetric.LATENCY,
        GatewayMetric.TOKEN_USAGE
    )
    private val installedInterceptors = mutableListOf<Interceptor>()

    fun enable(metric: GatewayMetric) {
        metrics += metric
    }

    fun disable(metric: GatewayMetric) {
        metrics -= metric
    }

    fun use(interceptor: Interceptor) {
        installedInterceptors += interceptor
    }

    fun telemetry(block: TelemetryMetricsInterceptorBuilder.() -> Unit) {
        telemetryMetricsInterceptorBuild(block).forEach(::use)
    }

    internal fun build(): MetricsConfig = MetricsConfig(
        enabled = enabled,
        enabledMetrics = metrics.toSet(),
        interceptors = installedInterceptors.toList()
    )
}

