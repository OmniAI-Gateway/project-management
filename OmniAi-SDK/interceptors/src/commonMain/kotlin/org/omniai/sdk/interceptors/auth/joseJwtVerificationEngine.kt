package org.omniai.sdk.interceptors.auth

import org.omniai.sdk.interceptors.auth.domain.AuthenticationDecision

fun interface JwtVerificationEngine {
    suspend fun verify(rawToken: String): AuthenticationDecision
}

expect fun joseJwtVerificationEngine(
    key: Any,
    issuer: String,
    audience: String
): JwtVerificationEngine