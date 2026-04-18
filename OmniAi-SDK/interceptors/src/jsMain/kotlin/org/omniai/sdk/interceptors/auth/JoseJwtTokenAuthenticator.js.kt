package org.omniai.sdk.interceptors.auth

import kotlinx.coroutines.await
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import org.omniai.sdk.interceptors.auth.domain.AuthenticationDecision
import kotlin.js.Promise

@JsModule("jose")
@JsNonModule
external object JoseLib {
    fun jwtVerify(token: String, keyResolver: dynamic, options: dynamic): Promise<dynamic>
}


@OptIn(DelicateCoroutinesApi::class)
actual fun joseJwtVerificationEngine(
    keysProvider: PublicKeysProvider,
    issuer: String,
    audience: String
): JwtVerificationEngine = JwtVerificationEngine { rawToken ->
    try {
        // 1. O resolver precisa converter a nossa função suspend numa Promise JS
        val keyResolver: (dynamic, dynamic) -> Promise<dynamic> = { header, _ ->
            val kid = header.kid as String?
            GlobalScope.promise {
                keysProvider.getPublicKey(issuer, kid)
            }
        }

        val options = js("{}")
        options.issuer = issuer
        options.audience = audience

        // 2. Aguarda a verificação
        val result = JoseLib.jwtVerify(rawToken, keyResolver, options).await()

        // 3. Extração segura dos claims de um objeto dinâmico JS
        val payload = result.payload
        val claims = mutableMapOf<String, Any>()

        // Forma segura de iterar propriedades em JS dinâmico no Kotlin
        val keys = js("Object.keys")(payload)
        val length = keys.length as Int
        for (i in 0 until length) {
            val key = keys[i] as String
            claims[key] = payload[key]
        }

        AuthenticationDecision.Allow(claims = claims)
    } catch (e: Exception) {
        AuthenticationDecision.Deny("Erro na validação JS: ${e.message}")
    }
}