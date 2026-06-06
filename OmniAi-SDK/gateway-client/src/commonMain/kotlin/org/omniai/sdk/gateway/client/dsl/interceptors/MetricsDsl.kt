package org.omniai.sdk.gateway.client.dsl.interceptors

import org.omniai.sdk.interceptors.metrics.MetricsInterceptor
import org.omniai.sdk.application.pipeline.Interceptor
import org.omniai.sdk.interceptors.metrics.CustomMetric
import org.omniai.sdk.interceptors.metrics.DefaultLatencyMetricConfig
import org.omniai.sdk.interceptors.metrics.DefaultLatencyMetricConfigBuilder
import org.omniai.sdk.interceptors.metrics.MetricsAttributeExtractor
import org.omniai.sdk.interceptors.metrics.MetricsAttributesBuilder
import org.omniai.sdk.interceptors.metrics.MetricsConfigurationBuilder
import org.omniai.sdk.interceptors.metrics.MetricsInterceptorConfig
import org.omniai.sdk.interceptors.metrics.MetricsPort
import org.omniai.sdk.interceptors.metrics.Tracer
import org.omniai.sdk.interceptors.metrics.TracingInterceptor
import org.omniai.sdk.gateway.client.dsl.GatewayDsl

@GatewayDsl
class MetricsInterceptorBuilder {
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
        val resolvedPort = requireNotNull(metricsPort) {
            "MetricsPort is required to build telemetry metrics interceptors"
        }

        val interceptors = mutableListOf<Interceptor>()
        tracer?.let { interceptors += TracingInterceptor(it) }

        interceptors += MetricsInterceptor(
            metricsPort = resolvedPort,
            config = MetricsInterceptorConfig(
                defaultLatency = defaultLatencyConfig,
                attributeExtractors = attributeExtractors,
                customMetrics = customMetrics
            )
        )
        return interceptors
    }
}

fun metricsInterceptorBuild(
    block: MetricsInterceptorBuilder.() -> Unit
): List<Interceptor> = MetricsInterceptorBuilder().apply(block).build()