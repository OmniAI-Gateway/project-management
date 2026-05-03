package org.omniai.sdk.interceptors.metrics

import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.core.pipeline.PipelineResult

typealias MetricsAttributeExtractor = (GatewayContext, PipelineResult?) -> Map<String, String>

data class DefaultLatencyMetricConfig(
    val name: String = "gateway.inference.request.duration",
    val enabled: Boolean = true,
    val additionalAttributes: List<MetricsAttributeExtractor> = emptyList()
)

data class MetricsInterceptorConfig(
    val defaultLatency: DefaultLatencyMetricConfig = DefaultLatencyMetricConfig(),
    val attributeExtractors: List<MetricsAttributeExtractor> = emptyList(),
    val customMetrics: List<CustomMetric> = emptyList()
)
