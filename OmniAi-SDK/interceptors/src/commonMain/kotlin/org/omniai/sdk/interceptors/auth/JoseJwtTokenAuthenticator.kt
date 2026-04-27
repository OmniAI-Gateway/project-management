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
            if (token.kind == TokenKind.OPAQUE) {
                val claims = infra.introspectToken(token.rawValue)
                AuthenticationDecision.Allow(claims)
            } else {
                // Se já for JWT, mantém a verificação local original
                // 1. Obtemos a chave pública (pode ser pelo issuer ou por um kid específico se tiver)
                val publicKey = infra.getPublicKey(expectedIssuer, null)
                joseJwtVerificationEngine(publicKey, expectedIssuer, expectedAudience).verify(token.rawValue)
            }
        } catch (e: Exception) {
            AuthenticationDecision.Deny("Falha na autenticação: ${e.message}")
        }
    }
}