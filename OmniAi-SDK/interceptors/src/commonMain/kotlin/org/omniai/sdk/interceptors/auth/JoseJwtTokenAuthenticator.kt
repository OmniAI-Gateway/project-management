package org.omniai.sdk.interceptors.auth

import org.omniai.sdk.core.pipeline.GatewayContext

fun interface JwtVerificationEngine {
    suspend fun verify(rawToken: String): AuthenticationDecision
}

expect fun joseJwtVerificationEngine(
    keysProvider: PublicKeysProvider,
    issuer: String,
    audience: String
): JwtVerificationEngine

class JoseJwtTokenAuthenticator(
    private val infra: AuthSecurityInfrastructure,
    private val expectedIssuer: String,
    private val expectedAudience: String,
) : TokenAuthenticator {

    override suspend fun authenticate(token: AuthToken, context: GatewayContext): AuthenticationDecision {
        return try {
            val jwtToValidate =
                if (token.kind == TokenKind.OPAQUE) infra.exchangeApiKey(token.rawValue) else token.rawValue

            /* percebe que precisa de uma chave pública para verificar a assinatura do jwtToValidate.
            Ele então olha para o objeto infra  e chama infra.getPublicKey(issuer, kid)
             */

            /*
            deveriamos implementar cache
            Ele pede a chave à infra uma vez, valida o JWT e guarda a chave num mapa interno
            (Map<String, PublicKey>) para não ter de pedir mais
             */

            joseJwtVerificationEngine(infra, expectedIssuer, expectedAudience).verify(jwtToValidate)

        } catch (e: Exception) {
            AuthenticationDecision.Deny("Falha na autenticação: ${e.message}")
        }
    }
}
