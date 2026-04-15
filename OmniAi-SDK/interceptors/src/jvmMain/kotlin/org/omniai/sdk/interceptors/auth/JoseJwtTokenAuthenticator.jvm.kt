package org.omniai.sdk.interceptors.auth

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.proc.JWSKeySelector
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import java.security.PublicKey

actual fun joseJwtVerificationEngine(
    keysProvider: PublicKeysProvider,
    issuer: String,
    audience: String
): JwtVerificationEngine = JwtVerificationEngine { rawToken ->
    try {
        val processor = DefaultJWTProcessor<SecurityContext>()

        // Configuramos o seletor de chaves para usar a nossa infra
        processor.jwsKeySelector = JWSKeySelector { header, _ ->
            val key = kotlinx.coroutines.runBlocking {
                keysProvider.getPublicKey(issuer, header.keyID)
            }
            // O Nimbus espera uma lista de chaves (Key)
            listOf(key as java.security.Key)
        }

        val claims: JWTClaimsSet = processor.process(rawToken, null)

        // Validações Manuais de Claims (Issuer e Audience)
        if (claims.issuer != issuer) {
            return@JwtVerificationEngine AuthenticationDecision.Deny("Issuer inválido")
        }
        if (!claims.audience.contains(audience)) {
            return@JwtVerificationEngine AuthenticationDecision.Deny("Audience inválida")
        }

        AuthenticationDecision.Allow
    } catch (e: Exception) {
        AuthenticationDecision.Deny("Erro na validação JVM: ${e.message}")
    }
}