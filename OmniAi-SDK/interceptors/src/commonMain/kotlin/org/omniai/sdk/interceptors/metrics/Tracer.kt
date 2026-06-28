package org.omniai.sdk.interceptors.metrics

interface Tracer {
    suspend fun <T> withSpan(
        spanName: String,
        block: suspend () -> T,
    ): T
}
