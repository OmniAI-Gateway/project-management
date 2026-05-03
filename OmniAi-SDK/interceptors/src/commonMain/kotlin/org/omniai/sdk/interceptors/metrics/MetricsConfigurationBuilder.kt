package org.omniai.sdk.interceptors.metrics

class MetricsConfigurationBuilder {
    private val configuredMetrics = mutableListOf<CustomMetric>()

    val metrics: List<CustomMetric>
        get() = configuredMetrics.toList()

    fun counter(name: String, description: String = "", block: MetricDefinitionBuilder.() -> Unit) {
        configuredMetrics += MetricDefinitionBuilder(name, InstrumentType.COUNTER, description).apply(block).build()
    }

    fun histogram(name: String, description: String = "", block: MetricDefinitionBuilder.() -> Unit) {
        configuredMetrics += MetricDefinitionBuilder(name, InstrumentType.HISTOGRAM, description).apply(block).build()
    }

    fun upDownCounter(name: String, description: String = "", block: MetricDefinitionBuilder.() -> Unit) {
        configuredMetrics += MetricDefinitionBuilder(name, InstrumentType.UP_DOWN_COUNTER, description).apply(block).build()
    }
}

fun metricsConfiguration(block: MetricsConfigurationBuilder.() -> Unit): List<CustomMetric> =
    MetricsConfigurationBuilder().apply(block).metrics
