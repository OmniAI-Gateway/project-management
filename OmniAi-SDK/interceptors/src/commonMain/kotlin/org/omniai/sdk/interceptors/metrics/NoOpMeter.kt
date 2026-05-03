package org.omniai.sdk.interceptors.metrics

object NoOpMeter : Meter {
    override fun recordLatency(metricName: String, durationMs: Double, attributes: Map<String, String>) {
        // No-Op
    }
}
