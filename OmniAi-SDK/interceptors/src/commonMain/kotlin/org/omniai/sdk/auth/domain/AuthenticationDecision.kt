package org.omniai.sdk.auth.domain

sealed interface AuthenticationDecision {
    data class Allow(val claims: Map<String, Any>) : AuthenticationDecision
    data class Deny(val reason: String) : AuthenticationDecision
}

