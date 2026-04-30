package org.omniai.sdk.auth.authenticators

import org.omniai.sdk.auth.domain.AuthenticationDecision
import org.omniai.sdk.auth.domain.AuthValidationResult
import org.omniai.sdk.auth.domain.AuthToken
import org.omniai.sdk.auth.domain.JWT
import org.omniai.sdk.auth.domain.TokenValidationParams
import org.omniai.sdk.auth.domain.Kid
import org.omniai.sdk.auth.domain.OPAQUE
import org.omniai.sdk.auth.interfaces.AuthSecurityInfrastructure
import org.omniai.sdk.auth.interfaces.JwtVerificationEngine
import org.omniai.sdk.auth.interfaces.TokenAuthenticator

class DefaultTokenAuthenticator(
    private val infra: AuthSecurityInfrastructure,
    private val jwtVerificationEngine: JwtVerificationEngine,
) : TokenAuthenticator {

    override suspend fun authenticate(
        token: AuthToken,
        params: TokenValidationParams?
    ): AuthenticationDecision {
        return try {
            when(token){
                is OPAQUE -> {
                    val result = infra.introspectToken(token.token)
                    if (result != null && result.active) {
                        AuthenticationDecision.Allow(AuthValidationResult.Opaque(result))
                    } else {
                        AuthenticationDecision.Deny("Token inativo ou inválido.")
                    }
                }
                is JWT ->   {
                    val kid = token.token.header.keyId ?: return AuthenticationDecision.Deny("No kid Passed")
                    val publicKey = infra.getPublicKey(Kid(kid))
                        ?: return AuthenticationDecision.Deny("No public key found of: $kid")
                    jwtVerificationEngine.verify(token.token, publicKey, params)
                }
            }
        } catch (e: Exception) {
            AuthenticationDecision.Deny("Auth Failed: ${e.message}")
        }
    }
}
