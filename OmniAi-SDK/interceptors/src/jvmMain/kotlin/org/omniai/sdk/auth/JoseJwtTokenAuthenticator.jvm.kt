package org.omniai.sdk.auth

import com.nimbusds.jose.proc.JWSKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import org.omniai.sdk.auth.domain.AuthenticationDecision
import org.omniai.sdk.auth.interfaces.JwtVerificationEngine
import java.security.Key

actual fun joseJwtVerificationEngine(
    key: Any,
    issuer: String,
    audience: String
): JwtVerificationEngine = JwtVerificationEngine { rawToken ->
    try {
        val processor = DefaultJWTProcessor<SecurityContext>()

        // O KeySelector agora ignora o header e retorna sempre a chave fornecida
        processor.jwsKeySelector = JWSKeySelector { _, _ ->
            listOf(key as Key)
        }

        val claims: JWTClaimsSet = processor.process(rawToken, null)

        // Validações de Issuer e Audience
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
