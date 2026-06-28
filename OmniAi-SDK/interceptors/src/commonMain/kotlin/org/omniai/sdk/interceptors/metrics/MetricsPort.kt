package org.omniai.sdk.interceptors.metrics

fun interface CounterMetric {
    fun add(
        value: Double,
        attributes: Map<String, String>,
    )
}

fun interface HistogramMetric {
    fun record(
        value: Double,
        attributes: Map<String, String>,
    )
}

fun interface UpDownCounterMetric {
    fun add(
        delta: Double,
        attributes: Map<String, String>,
    )
}

interface MetricsPort {
    fun counter(
        name: String,
        description: String,
        unit: String? = null,
    ): CounterMetric

    fun histogram(
        name: String,
        description: String,
        unit: String? = null,
    ): HistogramMetric

    fun upDownCounter(
        name: String,
        description: String,
        unit: String? = null,
    ): UpDownCounterMetric
}

object NoOpMetricsPort : MetricsPort {
    private val noopCounter = CounterMetric { _, _ -> }
    private val noopHistogram = HistogramMetric { _, _ -> }
    private val noopUpDownCounter = UpDownCounterMetric { _, _ -> }

    override fun counter(
        name: String,
        description: String,
        unit: String?,
    ): CounterMetric = noopCounter

    override fun histogram(
        name: String,
        description: String,
        unit: String?,
    ): HistogramMetric = noopHistogram

    override fun upDownCounter(
        name: String,
        description: String,
        unit: String?,
    ): UpDownCounterMetric = noopUpDownCounter
}
