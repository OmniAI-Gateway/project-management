package org.omniai.sdk.interceptors.auth.engines

import kotlinx.coroutines.await
import org.omniai.sdk.interceptors.auth.domain.AuthValidationResult
import org.omniai.sdk.interceptors.auth.domain.AuthenticationDecision
import org.omniai.sdk.interceptors.auth.domain.DecodedJwt
import org.omniai.sdk.interceptors.auth.domain.PublicKey
import org.omniai.sdk.interceptors.auth.domain.TokenValidationParams
import org.omniai.sdk.interceptors.auth.interfaces.JwtVerificationEngine
import kotlin.js.Promise

@JsModule("jose")
@JsNonModule
external object JoseLib {
    fun jwtVerify(
        token: String,
        key: dynamic,
        options: dynamic,
    ): Promise<dynamic>

    fun importSPKI(
        spki: String,
        alg: String,
    ): Promise<dynamic>
}

actual class PlatformJwtVerificationEngine actual constructor() : JwtVerificationEngine {
    actual override suspend fun verify(
        token: DecodedJwt,
        publicKey: PublicKey,
        params: TokenValidationParams?,
    ): AuthenticationDecision {
        return try {
            val pemKey =
                """
                -----BEGIN PUBLIC KEY-----
                ${publicKey.key.value} 
                -----END PUBLIC KEY-----
                """.trimIndent()

            val alg = if (publicKey.algorithm == "RSA") "RS256" else publicKey.algorithm

            val importedKey = JoseLib.importSPKI(pemKey, alg).await()

            JoseLib.jwtVerify(token.rawToken, importedKey, js("{}")).await()
            if (token.payload.issuer != params?.expectedIssuer) {
                return AuthenticationDecision.Deny("Issuer inválido. Esperado: ${params?.expectedIssuer}, Atual: ${token.payload.issuer}")
            }

            val expectedAud = params?.expectedAudience ?: ""
            if (token.payload.audience == null || !token.payload.audience.contains(expectedAud)) {
                return AuthenticationDecision.Deny("Audience inválida. Esperado: $expectedAud")
            }
            AuthenticationDecision.Allow(AuthValidationResult.Jwt(token))
        } catch (e: Exception) {
            AuthenticationDecision.Deny("Erro na validação JS (jose): ${e.message}")
        }
    }
}
