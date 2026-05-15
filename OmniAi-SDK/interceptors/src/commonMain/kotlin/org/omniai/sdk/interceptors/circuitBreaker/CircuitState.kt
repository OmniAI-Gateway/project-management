package org.omniai.sdk.interceptors.circuitBreaker

enum class CircuitState {
    CLOSED,    // Tudo a funcionar, chamadas passam
    OPEN,      // A falhar, chamadas são rejeitadas imediatamente
    HALF_OPEN  // Em testes, deixa passar X chamadas para testar a saúde
}