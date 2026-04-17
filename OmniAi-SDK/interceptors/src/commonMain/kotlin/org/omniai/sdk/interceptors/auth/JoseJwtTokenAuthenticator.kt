package org.omniai.sdk.interceptors.auth

import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.interceptors.auth.domain.AuthToken
import org.omniai.sdk.interceptors.auth.domain.AuthenticationDecision
import org.omniai.sdk.interceptors.auth.domain.TokenAuthenticator
import org.omniai.sdk.interceptors.auth.domain.TokenKind

class JoseJwtTokenAuthenticator(
    private val infra: AuthSecurityInfrastructure,
    private val expectedIssuer: String,
    private val expectedAudience: String,
) : TokenAuthenticator {

    override suspend fun authenticate(token: AuthToken, context: GatewayContext): AuthenticationDecision {
        return try {

            val jwtToValidate = if (token.kind == TokenKind.OPAQUE) {
                infra.exchangeApiKey(token.rawValue)
            } else {
                token.rawValue
            }

            joseJwtVerificationEngine(infra, expectedIssuer, expectedAudience).verify(jwtToValidate)

        } catch (e: Exception) {
            AuthenticationDecision.Deny("Falha na autenticação: ${e.message}")
        }
    }
}
