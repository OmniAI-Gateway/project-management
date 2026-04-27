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
    key: Any,
    issuer: String,
    audience: String
): JwtVerificationEngine = JwtVerificationEngine { rawToken ->
    try {
        val options = js("{}")
        options.issuer = issuer
        options.audience = audience

        // A biblioteca 'jose' permite passar a chave diretamente em vez do resolver
        val result = JoseLib.jwtVerify(rawToken, key, options).await()

        val payload = result.payload
        val claims = mutableMapOf<String, Any>()

        // Extração das claims do payload JS
        val keys = js("Object.keys")(payload)
        val length = keys.length as Int
        for (i in 0 until length) {
            val propertyName = keys[i] as String
            claims[propertyName] = payload[propertyName]
        }

        AuthenticationDecision.Allow(claims = claims)
    } catch (e: Exception) {
        AuthenticationDecision.Deny("Erro na validação JS: ${e.message}")
    }
}
