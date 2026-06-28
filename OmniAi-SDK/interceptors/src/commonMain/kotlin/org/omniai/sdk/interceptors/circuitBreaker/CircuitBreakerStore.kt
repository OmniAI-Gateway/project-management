package org.omniai.sdk.interceptors.circuitBreaker

interface CircuitBreakerStore {
    suspend fun getState(outboundId: String): CircuitState

    suspend fun getFailures(outboundId: String): Int

    suspend fun getLastFailureTime(outboundId: String): Long?

    // Operações de transição
    suspend fun recordFailure(outboundId: String)

    suspend fun recordSuccess(outboundId: String)

    suspend fun transitionState(
        outboundId: String,
        newState: CircuitState,
    )
}
