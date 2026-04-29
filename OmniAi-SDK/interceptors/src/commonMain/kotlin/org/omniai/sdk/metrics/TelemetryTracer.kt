package org.omniai.sdk.metrics

interface TelemetryTracer {
    suspend fun <T> withSpan(spanName: String, block: suspend () -> T): T
}
