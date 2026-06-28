package org.omniai.sdk.interceptors.circuitBreaker

data class CircuitBreakerConfig(
    val failureThreshold: Int = 5, // Falhas até abrir o circuito
    val resetTimeoutMs: Long = 10000L, // Tempo até tentar passar a HALF_OPEN
    val halfOpenTestRequests: Int = 2, // Quantas chamadas deixar passar no HALF_OPEN
)
