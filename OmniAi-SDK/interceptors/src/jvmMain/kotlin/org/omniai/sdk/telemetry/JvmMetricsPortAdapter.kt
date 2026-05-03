package org.omniai.sdk.telemetry

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.DoubleCounter
import io.opentelemetry.api.metrics.DoubleHistogram
import io.opentelemetry.api.metrics.DoubleUpDownCounter
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
    private val counters = ConcurrentHashMap<String, DoubleCounter>()
    private val histograms = ConcurrentHashMap<String, DoubleHistogram>()
    private val upDownCounters = ConcurrentHashMap<String, DoubleUpDownCounter>()

    override fun counter(name: String, description: String): CounterMetric =
        counterMetrics.getOrPut(name) {
            CounterMetric { value, attributes ->
                val instrument = counters.getOrPut(name) {
                    meter.counterBuilder(name)
                        .ofDoubles()
                        .setDescription(description)
                        .build()
                }
                instrument.add(value, attributes.toOtelAttributes())
            }
        }

    override fun histogram(name: String, description: String): HistogramMetric =
        histogramMetrics.getOrPut(name) {
            HistogramMetric { value, attributes ->
                val instrument = histograms.getOrPut(name) {
                    meter.histogramBuilder(name)
                        .setDescription(description)
                        .build()
                }
                instrument.record(value, attributes.toOtelAttributes())
            }
        }

    override fun upDownCounter(name: String, description: String): UpDownCounterMetric =
        upDownCounterMetrics.getOrPut(name) {
            UpDownCounterMetric { delta, attributes ->
                val instrument = upDownCounters.getOrPut(name) {
                    meter.upDownCounterBuilder(name)
                        .ofDoubles()
                        .setDescription(description)
                        .build()
                }
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
