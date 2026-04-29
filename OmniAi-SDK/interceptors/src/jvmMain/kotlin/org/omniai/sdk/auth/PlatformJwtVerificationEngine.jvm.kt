package org.omniai.sdk.auth

import com.nimbusds.jose.proc.JWSKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import org.omniai.sdk.auth.domain.AuthenticationDecision
import org.omniai.sdk.auth.domain.PublicKey
import org.omniai.sdk.auth.domain.TokenValidationParams
import org.omniai.sdk.auth.interfaces.JwtVerificationEngine
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual class PlatformJwtVerificationEngine actual constructor() : JwtVerificationEngine {

    @OptIn(ExperimentalEncodingApi::class)
    actual override suspend fun verify(
        token: String,
        publicKey: PublicKey,
        params: TokenValidationParams
    ): AuthenticationDecision {
        return try {
            val keyBytes = Base64.decode(publicKey.key)
            val spec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance(publicKey.algorithm)
            val javaPublicKey = keyFactory.generatePublic(spec)

            val processor = DefaultJWTProcessor<SecurityContext>()

            processor.jwsKeySelector = JWSKeySelector { _, _ ->
                listOf(javaPublicKey)
            }

            val claims: JWTClaimsSet = processor.process(token, null)

            if (claims.issuer != params.expectedIssuer) {
                return AuthenticationDecision.Deny("Issuer inválido. Esperado: ${params.expectedIssuer}, Atual: ${claims.issuer}")
            }
            if (claims.audience == null || !claims.audience.contains(params.expectedAudience)) {
                return AuthenticationDecision.Deny("Audience inválida. Esperado: ${params.expectedAudience}")
            }

            AuthenticationDecision.Allow(claims = claims.claims)

        } catch (e: Exception) {
            AuthenticationDecision.Deny("Erro na validação JVM (Nimbus JOSE): ${e.message}")
        }
    }
}