package org.omniai.gateway.app

import org.omniai.sdk.metrics.TelemetryMeter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
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
import org.omniai.sdk.metrics.NoOpTelemetryMeter
import org.omniai.sdk.metrics.NoOpTelemetryTracer
import org.omniai.sdk.telemetry.JvmTelemetryMeter
import org.omniai.sdk.telemetry.JvmTelemetryTracer
import java.time.Duration

private const val OTEL_SCOPE = "omniai-gateway-sdk"

fun buildTelemetryRuntime(config: GatewayConfig): TelemetryRuntime {
    if (!config.telemetryEnabled) {
        return TelemetryRuntime(meter = PrometheusLikeTelemetryMeter(), tracer = NoOpTelemetryTracer)
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

            val meter = JvmTelemetryMeter(openTelemetry, OTEL_SCOPE)
            val tracer = JvmTelemetryTracer(openTelemetry, OTEL_SCOPE)

            TelemetryRuntime(meter = meter, tracer = tracer)
        } catch (e: Exception) {
            println("Aviso: Falha ao inicializar OpenTelemetry. Usando fallback. Erro: ${e.message}")
            TelemetryRuntime(meter = NoOpTelemetryMeter, tracer = NoOpTelemetryTracer)
        }
    }

    return TelemetryRuntime(meter = NoOpTelemetryMeter, tracer = NoOpTelemetryTracer)
}

private fun initOpenTelemetrySdk(endpoint: String): OpenTelemetry {
    val metricExporter = OtlpGrpcMetricExporter.builder()
        .setEndpoint(endpoint)
        .build()

    val meterProvider = SdkMeterProvider.builder()
        .registerMetricReader(
            PeriodicMetricReader.builder(metricExporter)
                .setInterval(Duration.ofSeconds(1))
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

private class PrometheusLikeTelemetryMeter : TelemetryMeter {
    private val totalRequests = AtomicLong(0)
    private val totalLatencyByMetric = ConcurrentHashMap<String, Double>()

    override fun recordLatency(metricName: String, durationMs: Double, attributes: Map<String, String>) {
        totalRequests.incrementAndGet()
        totalLatencyByMetric.merge(metricName, durationMs) { current, add -> current + add }

        val labels = attributes.entries.joinToString(",") { (k, v) -> "$k=\"$v\"" }
        // Prometheus-like line format; replace by OTEL exporter integration when available.
        println("${metricName}_ms{$labels} $durationMs")
    }
}

