package org.omniai.sdk.telemetry

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import java.util.concurrent.ConcurrentHashMap
import org.omniai.sdk.interceptors.metrics.CounterMetric
import org.omniai.sdk.interceptors.metrics.HistogramMetric
import org.omniai.sdk.interceptors.metrics.MetricsPort
import org.omniai.sdk.interceptors.metrics.UpDownCounterMetric

class JvmMetricsPortAdapter(
    openTelemetry: OpenTelemetry,
    instrumentationScopeName: String = "omniai-gateway-sdk"
) : MetricsPort {

    private val meter = openTelemetry.getMeter(instrumentationScopeName)
    private val counterMetrics = ConcurrentHashMap<String, CounterMetric>()
    private val histogramMetrics = ConcurrentHashMap<String, HistogramMetric>()
    private val upDownCounterMetrics = ConcurrentHashMap<String, UpDownCounterMetric>()

    override fun counter(name: String, description: String, unit: String?): CounterMetric =
        counterMetrics.getOrPut(name) {
            val instrument = meter.counterBuilder(name)
                .ofDoubles()
                .setDescription(description)
                .apply { unit?.let { setUnit(it) } }
                .build()

            CounterMetric { value, attributes ->
                instrument.add(value, attributes.toOtelAttributes())
            }
        }

    override fun histogram(name: String, description: String, unit: String?): HistogramMetric =
        histogramMetrics.getOrPut(name) {
            val instrument = meter.histogramBuilder(name)
                .setDescription(description)
                .apply { unit?.let { setUnit(it) } }
                .build()

            HistogramMetric { value, attributes ->
                instrument.record(value, attributes.toOtelAttributes())
            }
        }

    override fun upDownCounter(name: String, description: String, unit: String?): UpDownCounterMetric =
        upDownCounterMetrics.getOrPut(name) {
            val instrument = meter.upDownCounterBuilder(name)
                .ofDoubles()
                .setDescription(description)
                .apply { unit?.let { setUnit(it) } }
                .build()

            UpDownCounterMetric { delta, attributes ->
                instrument.add(delta, attributes.toOtelAttributes())
            }
        }
}

private fun Map<String, String>.toOtelAttributes(): Attributes {
    val builder = Attributes.builder()
    forEach { (key, value) ->
        builder.put(AttributeKey.stringKey(key), value)
    }
    return builder.build()
}