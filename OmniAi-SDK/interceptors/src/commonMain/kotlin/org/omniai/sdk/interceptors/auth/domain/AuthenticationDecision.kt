package org.omniai.sdk.interceptors.auth.domain

import org.omniai.sdk.core.pipeline.GatewayContext

sealed interface AuthenticationDecision {
    data class Allow(val claims: Map<String, Any>) : AuthenticationDecision
    data class Deny(val reason: String) : AuthenticationDecision
}

interface TokenAuthenticator {
    suspend fun authenticate(token: AuthToken, context: GatewayContext): AuthenticationDecision
}
