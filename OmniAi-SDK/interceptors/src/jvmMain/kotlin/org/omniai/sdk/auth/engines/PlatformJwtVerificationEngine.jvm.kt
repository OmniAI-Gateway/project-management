package org.omniai.sdk.auth.engines

import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory

import com.nimbusds.jwt.SignedJWT
import org.omniai.sdk.auth.domain.AuthenticationDecision
import org.omniai.sdk.auth.domain.DecodedJwt
import org.omniai.sdk.auth.domain.PublicKey
import org.omniai.sdk.auth.domain.TokenValidationParams
import org.omniai.sdk.auth.domain.AuthValidationResult
import org.omniai.sdk.auth.interfaces.JwtVerificationEngine
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual class PlatformJwtVerificationEngine actual constructor() : JwtVerificationEngine {

    @OptIn(ExperimentalEncodingApi::class)
    actual override suspend fun verify(
        token: DecodedJwt,
        publicKey: PublicKey,
        params: TokenValidationParams?
    ): AuthenticationDecision {
        return try {
            // 1. Gerar a chave pública Java
            val keyBytes = Base64.decode(publicKey.key.value)
            val spec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance(publicKey.algorithm)
            val javaPublicKey = keyFactory.generatePublic(spec)

            val signedJWT = SignedJWT.parse(token.rawToken)

            val verifierFactory = DefaultJWSVerifierFactory()
            val verifier = verifierFactory.createJWSVerifier(signedJWT.header, javaPublicKey)

            if (!signedJWT.verify(verifier)) {
                return AuthenticationDecision.Deny("Assinatura do token inválida.")
            }

            if (token.payload.issuer != params?.expectedIssuer) {
                return AuthenticationDecision.Deny("Issuer inválido. Esperado: ${params?.expectedIssuer}, Atual: ${token.payload.issuer}")
            }

            val expectedAud = params?.expectedAudience ?: ""
            if (token.payload.audience == null || !token.payload.audience.contains(expectedAud)) {
                return AuthenticationDecision.Deny("Audience inválida. Esperado: $expectedAud")
            }

            AuthenticationDecision.Allow(AuthValidationResult.Jwt(token))

        } catch (e: Exception) {
            AuthenticationDecision.Deny("Erro na validação: ${e.message}")
        }
    }
}
