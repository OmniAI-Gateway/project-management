package org.omniai.sdk.interceptors.metrics

import org.omniai.sdk.application.pipeline.GatewayContext
import org.omniai.sdk.application.pipeline.PipelineResult
import org.omniai.sdk.common.AttributeKey
import org.omniai.sdk.common.TypedMap

class MetricsAttributesBuilder {
    private val extractors = mutableListOf<MetricsAttributeExtractor>()

    fun include(
        key: AttributeKey<*>,
        alias: String = key.name,
    ) {
        extractors += { context, _ ->
            context.attributes
                .readAny(key)
                ?.toMetricTag()
                ?.let { mapOf(alias to it) }
                ?: emptyMap()
        }
    }

    fun attribute(
        name: String,
        extractor: (GatewayContext, PipelineResult?) -> String?,
    ) {
        extractors += { context, result ->
            extractor(context, result)?.let { mapOf(name to it) } ?: emptyMap()
        }
    }

    fun build(): List<MetricsAttributeExtractor> = extractors.toList()
}

private fun Any.toMetricTag(): String? =
    when (this) {
        is String -> this
        is Number -> toString()
        is Boolean -> toString()
        else -> null
    }

@Suppress("UNCHECKED_CAST")
private fun TypedMap.readAny(key: AttributeKey<*>): Any? = this[key as AttributeKey<Any>]
