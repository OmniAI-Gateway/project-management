package org.omniai.sdk.core.pipeline

actual fun joseJwtVerificationEngine(config: JwtAuthConfig): JwtVerificationEngine {
    return JwtVerificationEngine {
        AuthenticationDecision.Deny("JWT verification is not available on JS target")
    }
}

