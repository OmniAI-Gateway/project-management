package org.omniai.sdk.interceptors.metrics

interface Meter {
    fun recordLatency(metricName: String, durationMs: Double, attributes: Map<String, String>)
}
