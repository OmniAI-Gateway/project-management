package org.omniai.sdk.interceptors.metrics

class DefaultLatencyMetricConfigBuilder(
    config: DefaultLatencyMetricConfig = DefaultLatencyMetricConfig()
) {
    var name: String = config.name
    var enabled: Boolean = config.enabled

    private val additionalAttributes = config.additionalAttributes.toMutableList()

    fun attributes(block: MetricsAttributesBuilder.() -> Unit) {
        additionalAttributes += MetricsAttributesBuilder().apply(block).build()
    }

    fun build(): DefaultLatencyMetricConfig = DefaultLatencyMetricConfig(
        name = name,
        enabled = enabled,
        additionalAttributes = additionalAttributes.toList()
    )
}
