package org.omniai.sdk.interceptors.metrics

import org.omniai.sdk.application.pipeline.GatewayContext
import org.omniai.sdk.application.pipeline.PipelineResult

typealias MetricsAttributeExtractor = (GatewayContext, PipelineResult?) -> Map<String, String>

data class DefaultLatencyMetricConfig(
    val name: String = "gateway.inference.request.duration",
    val enabled: Boolean = true,
    val additionalAttributes: List<MetricsAttributeExtractor> = emptyList()
)

data class ActiveRequestsConfig(
    val name: String = "gateway.requests.active",
    val enabled: Boolean = true
)

data class TtftConfig(
    val name: String = "gateway.inference.ttft",
    val enabled: Boolean = true
)

data class MetricsInterceptorConfig(
    val defaultLatency: DefaultLatencyMetricConfig = DefaultLatencyMetricConfig(),
    val activeRequests: ActiveRequestsConfig = ActiveRequestsConfig(),
    val ttft: TtftConfig = TtftConfig(),
    val attributeExtractors: List<MetricsAttributeExtractor> = emptyList(),
    val customMetrics: List<CustomMetric> = emptyList()
)
