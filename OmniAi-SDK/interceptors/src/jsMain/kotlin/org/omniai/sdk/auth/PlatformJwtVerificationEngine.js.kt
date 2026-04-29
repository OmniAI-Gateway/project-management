package org.omniai.sdk.auth

import kotlinx.coroutines.await
import org.omniai.sdk.auth.domain.AuthenticationDecision
import org.omniai.sdk.auth.domain.PublicKey
import org.omniai.sdk.auth.domain.TokenValidationParams
import org.omniai.sdk.auth.interfaces.JwtVerificationEngine
import kotlin.js.Promise

@JsModule("jose")
@JsNonModule
external object JoseLib {
    fun jwtVerify(token: String, key: dynamic, options: dynamic): Promise<dynamic>

    fun importSPKI(spki: String, alg: String): Promise<dynamic>
}

actual class PlatformJwtVerificationEngine actual constructor() : JwtVerificationEngine {

    actual override suspend fun verify(
        token: String,
        publicKey: PublicKey,
        params: TokenValidationParams
    ): AuthenticationDecision {
        return try {
            // 2. Reconstruir a chave no JS
            // A biblioteca 'jose' aceita chaves públicas em formato PEM.
            // Como guardamos a chave em Base64 no commonMain, basta embrulhá-la:
            val pemKey = """
                -----BEGIN PUBLIC KEY-----
                ${publicKey.key}
                -----END PUBLIC KEY-----
            """.trimIndent()

            val alg = if (publicKey.algorithm == "RSA") "RS256" else publicKey.algorithm

            val importedKey = JoseLib.importSPKI(pemKey, alg).await()

            val options = js("{}")
            options.issuer = params.expectedIssuer
            options.audience = params.expectedAudience

            val result = JoseLib.jwtVerify(token, importedKey, options).await()

            val payload = result.payload
            val claims = mutableMapOf<String, Any>()

            val keys = js("Object.keys")(payload)
            val length = keys.length as Int
            for (i in 0 until length) {
                val propertyName = keys[i] as String
                claims[propertyName] = payload[propertyName]
            }

            AuthenticationDecision.Allow(claims = claims)
        } catch (e: Exception) {
            AuthenticationDecision.Deny("Erro na validação JS (jose): ${e.message}")
        }
    }
}