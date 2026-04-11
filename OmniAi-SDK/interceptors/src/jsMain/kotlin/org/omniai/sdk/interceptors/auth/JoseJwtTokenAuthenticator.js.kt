@file:Suppress("UnsafeCastFromDynamic")

package org.omniai.sdk.interceptors.auth

import kotlinx.coroutines.await
import kotlin.js.Promise

actual fun joseJwtVerificationEngine(config: JwtAuthConfig): JwtVerificationEngine {
    val jwks = JoseInterop.createRemoteJWKSet(JsUrl(config.jwksUrl))

    return JwtVerificationEngine { rawToken ->
        try {
            val options = js("({})")
            options.issuer = config.issuer
            options.audience = config.audience
            options.algorithms = arrayOf(config.allowedAlgorithm)
            options.clockTolerance = config.clockSkewSeconds

            JoseInterop.jwtVerify(rawToken, jwks, options).await()
            AuthenticationDecision.Allow
        } catch (error: dynamic) {
            val errorDyn = error.asDynamic()
            val code = errorDyn.code as? String
            when (code) {
                "ERR_JWT_EXPIRED" -> AuthenticationDecision.Deny("JWT expired")
                "ERR_JWT_CLAIM_VALIDATION_FAILED" -> {
                    val claim = errorDyn.claim as? String
                    when (claim) {
                        "iss" -> AuthenticationDecision.Deny("Invalid JWT issuer")
                        "aud" -> AuthenticationDecision.Deny("Invalid JWT audience")
                        "nbf" -> AuthenticationDecision.Deny("JWT not active yet")
                        else -> AuthenticationDecision.Deny("Invalid JWT claims")
                    }
                }
                "ERR_JOSE_ALG_NOT_ALLOWED" -> AuthenticationDecision.Deny("Unsupported JWT algorithm")
                "ERR_JWS_SIGNATURE_VERIFICATION_FAILED" -> AuthenticationDecision.Deny("Invalid JWT signature")
                else -> {
                    val message = (errorDyn.message as? String) ?: "JWT verification failed"
                    AuthenticationDecision.Deny(message)
                }
            }
        }
    }
}

private external class JsUrl(url: String)

@JsModule("jose")
@JsNonModule
private external object JoseInterop {
    fun createRemoteJWKSet(url: dynamic): dynamic
    fun jwtVerify(token: String, key: dynamic, options: dynamic = definedExternally): Promise<dynamic>
}

