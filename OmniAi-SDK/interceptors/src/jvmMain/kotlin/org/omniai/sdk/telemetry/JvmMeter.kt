package org.omniai.sdk.telemetry

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.DoubleHistogram
import java.util.concurrent.ConcurrentHashMap
import org.omniai.sdk.interceptors.metrics.Meter

class JvmMeter(
    openTelemetry: OpenTelemetry,
    instrumentationScopeName: String = "omniai-gateway-sdk"
) : Meter {

    private val meter = openTelemetry.getMeter(instrumentationScopeName)
    private val histograms = ConcurrentHashMap<String, DoubleHistogram>()

    override fun recordLatency(metricName: String, durationMs: Double, attributes: Map<String, String>) {
        val histogram = histograms.getOrPut(metricName) {
            meter.histogramBuilder(metricName)
                .setUnit("ms")
                .setDescription("Request latency in milliseconds")
                .build()
        }

        val otelAttributesBuilder = Attributes.builder()
        attributes.forEach { (key, value) ->
            otelAttributesBuilder.put(AttributeKey.stringKey(key), value)
        }

        histogram.record(durationMs, otelAttributesBuilder.build())
    }
}
