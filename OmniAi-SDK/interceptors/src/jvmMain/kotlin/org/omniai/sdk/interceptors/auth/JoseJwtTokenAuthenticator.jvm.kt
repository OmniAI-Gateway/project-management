package org.omniai.sdk.interceptors.auth

import com.nimbusds.jose.proc.JWSKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import kotlinx.coroutines.runBlocking
import org.omniai.sdk.interceptors.auth.domain.AuthenticationDecision
import java.security.Key

actual fun joseJwtVerificationEngine(
    keysProvider: PublicKeysProvider,
    issuer: String,
    audience: String
): JwtVerificationEngine = JwtVerificationEngine { rawToken ->
    try {
        val processor = DefaultJWTProcessor<SecurityContext>()

        processor.jwsKeySelector = JWSKeySelector { header, _ ->
            val key = runBlocking {
                keysProvider.getPublicKey(issuer, header.keyID)
            }
            listOf(key as Key)
        }

        val claims: JWTClaimsSet = processor.process(rawToken, null)

        // Validações
        if (claims.issuer != issuer) {
            return@JwtVerificationEngine AuthenticationDecision.Deny("Issuer inválido")
        }
        if (!claims.audience.contains(audience)) {
            return@JwtVerificationEngine AuthenticationDecision.Deny("Audience inválida")
        }

        AuthenticationDecision.Allow(claims = claims.claims)

    } catch (e: Exception) {
        AuthenticationDecision.Deny("Erro na validação JVM: ${e.message}")
    }
}