package org.omniai.sdk.metrics
object NoOpTelemetryMeter : TelemetryMeter {
    override fun recordLatency(metricName: String, durationMs: Double, attributes: Map<String, String>) {
        // No-Op
    }
}