package org.omniai.gateway.metrics

interface TelemetryMeter {
    fun recordLatency(metricName: String, durationMs: Double, attributes: Map<String, String>)
}

object NoOpTelemetryMeter : TelemetryMeter {
    override fun recordLatency(metricName: String, durationMs: Double, attributes: Map<String, String>) {
        // No-Op
    }
}
