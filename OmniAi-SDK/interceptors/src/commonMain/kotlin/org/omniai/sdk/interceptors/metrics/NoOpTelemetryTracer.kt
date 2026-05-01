package org.omniai.sdk.interceptors.metrics

object NoOpTelemetryTracer : TelemetryTracer {
    override suspend fun <T> withSpan(spanName: String, block: suspend () -> T): T {
        // No-Op
        return block()
    }
}

