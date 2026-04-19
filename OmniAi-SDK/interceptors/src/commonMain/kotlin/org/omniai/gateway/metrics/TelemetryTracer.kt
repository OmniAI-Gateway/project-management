package org.omniai.gateway.metrics

interface TelemetryTracer {
    suspend fun <T> withSpan(spanName: String, block: suspend () -> T): T
}


object NoOpTelemetryTracer : TelemetryTracer {
    override suspend fun <T> withSpan(spanName: String, block: suspend () -> T): T {
        // No-Op
        return block()
    }
}