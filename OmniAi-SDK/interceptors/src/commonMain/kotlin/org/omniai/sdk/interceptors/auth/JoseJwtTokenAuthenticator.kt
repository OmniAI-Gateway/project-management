package org.omniai.sdk.interceptors.auth

import org.omniai.sdk.core.pipeline.GatewayContext

data class JwtAuthConfig(
    val issuer: String,
    val audience: String,
    val jwksUrl: String,
    val allowedAlgorithm: String = "RS256",
    val clockSkewSeconds: Long = 60,
    val connectTimeoutMillis: Int = 2_000,
    val readTimeoutMillis: Int = 2_000
)

fun interface JwtVerificationEngine {
    suspend fun verify(rawToken: String): AuthenticationDecision
}

expect fun joseJwtVerificationEngine(config: JwtAuthConfig): JwtVerificationEngine

class JoseJwtTokenAuthenticator(
    config: JwtAuthConfig
) : TokenAuthenticator {

    private val verificationEngine = joseJwtVerificationEngine(config)

    override suspend fun authenticate(token: AuthToken, context: GatewayContext): AuthenticationDecision {
        if (token.kind == TokenKind.OPAQUE) {
            return AuthenticationDecision.Allow
        }

        return verificationEngine.verify(token.rawValue)
    }
}

