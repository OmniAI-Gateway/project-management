package org.omniai.sdk.interceptors.metrics

import org.omniai.sdk.application.pipeline.GatewayContext
import org.omniai.sdk.application.pipeline.PipelineResult

enum class InstrumentType {
    COUNTER,
    UP_DOWN_COUNTER,
    HISTOGRAM
}

data class CustomMetric(
    val name: String,
    val type: InstrumentType,
    val description: String = "",
    val unit: String = "",
    val extractor: (GatewayContext, PipelineResult?) -> Double?,
    val attributes: (GatewayContext, PipelineResult?) -> Map<String, String> = { _, _ -> emptyMap() }
)
