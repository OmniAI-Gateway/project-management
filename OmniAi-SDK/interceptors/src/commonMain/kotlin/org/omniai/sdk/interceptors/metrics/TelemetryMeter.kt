package org.omniai.sdk.interceptors.metrics

interface TelemetryMeter {
    fun recordLatency(metricName: String, durationMs: Double, attributes: Map<String, String>)
}

