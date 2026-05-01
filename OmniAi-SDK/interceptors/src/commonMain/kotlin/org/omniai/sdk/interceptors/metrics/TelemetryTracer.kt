package org.omniai.sdk.interceptors.metrics

interface TelemetryTracer {
    suspend fun <T> withSpan(spanName: String, block: suspend () -> T): T
}
