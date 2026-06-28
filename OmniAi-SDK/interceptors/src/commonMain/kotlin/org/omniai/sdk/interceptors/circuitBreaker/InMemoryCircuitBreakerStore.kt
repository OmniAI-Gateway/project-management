package org.omniai.sdk.interceptors.circuitBreaker

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryCircuitBreakerStore : CircuitBreakerStore {
    private data class StateData(
        var state: CircuitState = CircuitState.CLOSED,
        var failures: Int = 0,
        var lastFailureTime: Long? = null,
    )

    private val store = mutableMapOf<String, StateData>()
    private val mutex = Mutex()

    override suspend fun getState(outboundId: String): CircuitState =
        mutex.withLock {
            store[outboundId]?.state ?: CircuitState.CLOSED
        }

    override suspend fun getFailures(outboundId: String): Int =
        mutex.withLock {
            store[outboundId]?.failures ?: 0
        }

    override suspend fun getLastFailureTime(outboundId: String): Long? =
        mutex.withLock {
            store[outboundId]?.lastFailureTime
        }

    override suspend fun recordFailure(outboundId: String) {
        mutex.withLock {
            val data = store.getOrPut(outboundId) { StateData() }
            data.failures += 1
            // TODO: Use a proper clock in production, but for in-memory this is ok
            data.lastFailureTime = 0L // or system clock
        }
    }

    override suspend fun recordSuccess(outboundId: String) {
        mutex.withLock {
            val data = store.getOrPut(outboundId) { StateData() }
            data.failures = 0
        }
    }

    override suspend fun transitionState(
        outboundId: String,
        newState: CircuitState,
    ) {
        mutex.withLock {
            val data = store.getOrPut(outboundId) { StateData() }
            data.state = newState
            if (newState == CircuitState.CLOSED) {
                data.failures = 0
            }
        }
    }
}
