package org.omniai.sdk.interceptors.metrics

import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.core.pipeline.PipelineResult

class MetricDefinitionBuilder(
    private val name: String,
    private val type: InstrumentType,
    private val description: String
) {
    private var extractor: (GatewayContext, PipelineResult?) -> Double? = { _, _ -> null }
    private var attributes: (GatewayContext, PipelineResult?) -> Map<String, String> = { _, _ -> emptyMap() }

    fun value(logic: (GatewayContext, PipelineResult?) -> Number?) {
        extractor = { context, result -> logic(context, result)?.toDouble() }
    }

    fun tags(logic: (GatewayContext, PipelineResult?) -> Map<String, String>) {
        attributes = logic
    }

    fun build(): CustomMetric = CustomMetric(
        name = name,
        type = type,
        description = description,
        extractor = extractor,
        attributes = attributes
    )
}
