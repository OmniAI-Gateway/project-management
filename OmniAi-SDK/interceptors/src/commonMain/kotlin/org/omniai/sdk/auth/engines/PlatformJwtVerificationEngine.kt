package org.omniai.sdk.auth.engines

import org.omniai.sdk.auth.domain.AuthenticationDecision
import org.omniai.sdk.auth.domain.PublicKey
import org.omniai.sdk.auth.domain.TokenValidationParams
import org.omniai.sdk.auth.interfaces.JwtVerificationEngine

expect class PlatformJwtVerificationEngine() : JwtVerificationEngine {
    override suspend fun verify(
        token: String,
        publicKey: PublicKey,
        params: TokenValidationParams?
    ): AuthenticationDecision
}
