package org.omniai.sdk.interceptors.metrics

object NoOpTracer : Tracer {
    override suspend fun <T> withSpan(
        spanName: String,
        block: suspend () -> T,
    ): T = block()
}
