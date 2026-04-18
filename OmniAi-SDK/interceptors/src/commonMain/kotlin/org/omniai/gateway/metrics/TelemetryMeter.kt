package org.omniai.gateway.metrics

interface TelemetryMeter {
    fun recordLatency(metricName: String, durationMs: Double, attributes: Map<String, String>)
}
