package org.omniai.sdk.gateway.client.dsl.interceptors

import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.interceptors.metrics.CustomMetric
import org.omniai.sdk.interceptors.metrics.DefaultLatencyMetricConfig
import org.omniai.sdk.interceptors.metrics.DefaultLatencyMetricConfigBuilder
import org.omniai.sdk.interceptors.metrics.MetricsAttributeExtractor
import org.omniai.sdk.interceptors.metrics.MetricsAttributesBuilder
import org.omniai.sdk.interceptors.metrics.MetricsConfigurationBuilder
import org.omniai.sdk.interceptors.metrics.MetricsInterceptor
import org.omniai.sdk.interceptors.metrics.MetricsInterceptorConfig
import org.omniai.sdk.interceptors.metrics.MetricsPort
import org.omniai.sdk.interceptors.metrics.Meter
import org.omniai.sdk.interceptors.metrics.Tracer
import org.omniai.sdk.interceptors.metrics.TracingInterceptor

class TelemetryMetricsInterceptorBuilder {
    var meter: Meter? = null
    var metricsPort: MetricsPort? = null
    var tracer: Tracer? = null
    private var defaultLatencyConfig: DefaultLatencyMetricConfig = DefaultLatencyMetricConfig()
    private var attributeExtractors: List<MetricsAttributeExtractor> = emptyList()
    private var customMetrics: List<CustomMetric> = emptyList()

    fun attributes(block: MetricsAttributesBuilder.() -> Unit) {
        attributeExtractors = MetricsAttributesBuilder().apply(block).build()
    }

    fun defaultLatency(block: DefaultLatencyMetricConfigBuilder.() -> Unit) {
        defaultLatencyConfig = DefaultLatencyMetricConfigBuilder(defaultLatencyConfig).apply(block).build()
    }

    fun customMetrics(block: MetricsConfigurationBuilder.() -> Unit) {
        customMetrics = MetricsConfigurationBuilder().apply(block).metrics.toList()
    }

    internal fun build(): List<Interceptor> {
        val resolvedMeter = requireNotNull(meter) {
            "Meter is required to build telemetry metrics interceptors"
        }
        if (customMetrics.isNotEmpty()) {
            requireNotNull(metricsPort) {
                "MetricsPort is required when customMetrics are configured."
            }
        }

        val interceptors = mutableListOf<Interceptor>()
        tracer?.let { interceptors += TracingInterceptor(it) }
        interceptors += MetricsInterceptor(
            meter = resolvedMeter,
            metricsPort = metricsPort,
            config = MetricsInterceptorConfig(
                defaultLatency = defaultLatencyConfig,
                attributeExtractors = attributeExtractors,
                customMetrics = customMetrics
            )
        )
        return interceptors
    }
}

fun telemetryMetricsInterceptorBuild(
    block: TelemetryMetricsInterceptorBuilder.() -> Unit
): List<Interceptor> = TelemetryMetricsInterceptorBuilder().apply(block).build()
