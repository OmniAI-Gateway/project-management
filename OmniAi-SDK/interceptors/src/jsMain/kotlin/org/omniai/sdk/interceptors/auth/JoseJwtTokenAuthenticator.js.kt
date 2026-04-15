package org.omniai.sdk.interceptors.auth

actual fun joseJwtVerificationEngine(
    keysProvider: PublicKeysProvider,
    issuer: String,
    audience: String
): JwtVerificationEngine = JwtVerificationEngine { rawToken ->
    TODO()
}