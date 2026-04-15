package org.omniai.sdk.interceptors.auth

import kotlinx.coroutines.await
import kotlin.js.Promise


@JsModule("jose")
@JsNonModule
external object JoseLib {
    fun jwtVerify(token: String, keyResolver: dynamic, options: dynamic): Promise<dynamic>
    fun importJWK(jwk: dynamic, alg: String): Promise<dynamic>
}

actual fun joseJwtVerificationEngine(
    keysProvider: PublicKeysProvider,
    issuer: String,
    audience: String
): JwtVerificationEngine = JwtVerificationEngine { rawToken ->
    try {
        val keyResolver: suspend (dynamic, dynamic) -> dynamic = { header, _ ->
            val kid = header.kid as String?
            keysProvider.getPublicKey(issuer, kid)
        }

        // Configurações de validação
        val options = js("{}")
        options.issuer = issuer
        options.audience = audience

        JoseLib.jwtVerify(rawToken, keyResolver, options).await()

        AuthenticationDecision.Allow
    } catch (e: Exception) {
        AuthenticationDecision.Deny("Erro na validação JS: ${e.message}")
    }
}