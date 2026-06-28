package org.omniai.sdk.interceptors.auth.domain

sealed interface AuthValidationResult {
    data class Jwt(
        val decoded: DecodedJwt,
    ) : AuthValidationResult

    data class Opaque(
        val introspectionResult: IntrospectionResult,
    ) : AuthValidationResult

    data class PassThrough(
        val token: AuthToken,
    ) : AuthValidationResult
}

sealed interface AuthenticationDecision {
    data class Allow(
        val data: AuthValidationResult,
    ) : AuthenticationDecision

    data class Deny(
        val reason: String,
    ) : AuthenticationDecision
}
