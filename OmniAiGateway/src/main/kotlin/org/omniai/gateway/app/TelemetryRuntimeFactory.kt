package org.omniai.gateway.app

import java.util.concurrent.ConcurrentHashMap
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.instrumentation.runtimemetrics.java8.Classes
import io.opentelemetry.instrumentation.runtimemetrics.java8.Cpu
import io.opentelemetry.instrumentation.runtimemetrics.java8.GarbageCollector
import io.opentelemetry.instrumentation.runtimemetrics.java8.MemoryPools
import io.opentelemetry.instrumentation.runtimemetrics.java8.Threads
import org.omniai.sdk.interceptors.metrics.NoOpTracer
import org.omniai.sdk.interceptors.metrics.MetricsPort
import org.omniai.sdk.interceptors.metrics.NoOpMetricsPort
import org.omniai.sdk.interceptors.metrics.CounterMetric
import org.omniai.sdk.interceptors.metrics.HistogramMetric
import org.omniai.sdk.interceptors.metrics.UpDownCounterMetric
import io.opentelemetry.api.common.Attributes
import org.omniai.sdk.telemetry.JvmTracer
import java.time.Duration

private const val OTEL_SCOPE = "omniai-gateway-sdk"

fun buildTelemetryRuntime(config: GatewayConfig): TelemetryRuntime {
    if (!config.telemetryEnabled) {
        return TelemetryRuntime(metricsPort = PrometheusLikeMetricsPort(), tracer = NoOpTracer)
    }

    val collectorEndpoint = config.otelCollectorEndpoint
    if (config.otelEnabled && !collectorEndpoint.isNullOrBlank()) {
        return try {
            val openTelemetry = initOpenTelemetrySdk(collectorEndpoint)
            Cpu.registerObservers(openTelemetry)
            MemoryPools.registerObservers(openTelemetry)
            Threads.registerObservers(openTelemetry)
            Classes.registerObservers(openTelemetry)
            GarbageCollector.registerObservers(openTelemetry)

            val metricsPort = JvmMetricsPort(openTelemetry, OTEL_SCOPE)
            val tracer = JvmTracer(openTelemetry, OTEL_SCOPE)

            TelemetryRuntime(metricsPort = metricsPort, tracer = tracer)
        } catch (e: Exception) {
            println("Aviso: Falha ao inicializar OpenTelemetry. Usando fallback. Erro: ${e.message}")
            TelemetryRuntime(metricsPort = NoOpMetricsPort, tracer = NoOpTracer)
        }
    }

    return TelemetryRuntime(metricsPort = NoOpMetricsPort, tracer = NoOpTracer)
}

private fun initOpenTelemetrySdk(endpoint: String): OpenTelemetry {
    val metricExporter = OtlpGrpcMetricExporter.builder()
        .setEndpoint(endpoint)
        .build()

    val meterProvider = SdkMeterProvider.builder()
        .registerMetricReader(
            PeriodicMetricReader.builder(metricExporter)
                .setInterval(Duration.ofSeconds(10))
                .build()
        )
        .build()

    val spanExporter = OtlpGrpcSpanExporter.builder()
        .setEndpoint(endpoint)
        .build()

    val tracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
        .build()


    return OpenTelemetrySdk.builder()
        .setMeterProvider(meterProvider)
        .setTracerProvider(tracerProvider)
        .build()
}

class JvmMetricsPort(openTelemetry: OpenTelemetry, scopeName: String) : MetricsPort {
    private val otelMeter = openTelemetry.getMeter(scopeName)

    override fun counter(name: String, description: String, unit: String?): CounterMetric {
        val builder = otelMeter.counterBuilder(name).setDescription(description)
        unit?.let { builder.setUnit(it) }
        val otelCounter = builder.build()
        return CounterMetric { value, attributes ->
            otelCounter.add(value.toLong(), toOtelAttributes(attributes))
        }
    }

    override fun histogram(name: String, description: String, unit: String?): HistogramMetric {
        val builder = otelMeter.histogramBuilder(name).setDescription(description)
        unit?.let { builder.setUnit(it) }
        val otelHistogram = builder.build()
        return HistogramMetric { value, attributes ->
            otelHistogram.record(value, toOtelAttributes(attributes))
        }
    }

    override fun upDownCounter(name: String, description: String, unit: String?): UpDownCounterMetric {
        val builder = otelMeter.upDownCounterBuilder(name).setDescription(description)
        unit?.let { builder.setUnit(it) }
        val otelUpDown = builder.build()
        return UpDownCounterMetric { delta, attributes ->
            otelUpDown.add(delta.toLong(), toOtelAttributes(attributes))
        }
    }

    private fun toOtelAttributes(attributes: Map<String, String>): Attributes {
        val builder = Attributes.builder()
        attributes.forEach { (k, v) -> builder.put(k, v) }
        return builder.build()
    }
}

private class PrometheusLikeMetricsPort : MetricsPort {
    private val metricsMap = ConcurrentHashMap<String, PrometheusLikeInstrument>()

    abstract class PrometheusLikeInstrument {
        abstract fun record(value: Double, attributes: Map<String, String>, type: String, name: String)
        protected fun printMetric(name: String, value: Double, attributes: Map<String, String>, type: String) {
            val labels = attributes.entries.joinToString(",") { (k, v) -> "$k=\"$v\"" }
            println("${name}_${type}{$labels} $value")
        }
    }

    override fun counter(name: String, description: String, unit: String?): CounterMetric {
        val instrument = metricsMap.getOrPut(name) { 
            object : PrometheusLikeInstrument() {
                var total = 0.0
                override fun record(value: Double, attributes: Map<String, String>, type: String, name: String) {
                    total += value
                    printMetric(name, total, attributes, "total")
                }
            }
        }
        return CounterMetric { value, attributes -> instrument.record(value, attributes, "counter", name) }
    }

    override fun histogram(name: String, description: String, unit: String?): HistogramMetric {
        val instrument = metricsMap.getOrPut(name) { 
            object : PrometheusLikeInstrument() {
                override fun record(value: Double, attributes: Map<String, String>, type: String, name: String) {
                    printMetric(name, value, attributes, "ms")
                }
            }
        }
        return HistogramMetric { value, attributes -> instrument.record(value, attributes, "histogram", name) }
    }

    override fun upDownCounter(name: String, description: String, unit: String?): UpDownCounterMetric {
        val instrument = metricsMap.getOrPut(name) { 
            object : PrometheusLikeInstrument() {
                var current = 0.0
                override fun record(value: Double, attributes: Map<String, String>, type: String, name: String) {
                    current += value
                    printMetric(name, current, attributes, "current")
                }
            }
        }
        return UpDownCounterMetric { delta, attributes -> instrument.record(delta, attributes, "updown", name) }
    }
}
