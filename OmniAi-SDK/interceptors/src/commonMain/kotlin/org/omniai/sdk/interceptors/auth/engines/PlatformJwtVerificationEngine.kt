package org.omniai.sdk.interceptors.auth.engines

import org.omniai.sdk.interceptors.auth.domain.AuthenticationDecision
import org.omniai.sdk.interceptors.auth.domain.DecodedJwt
import org.omniai.sdk.interceptors.auth.domain.PublicKey
import org.omniai.sdk.interceptors.auth.domain.TokenValidationParams
import org.omniai.sdk.interceptors.auth.interfaces.JwtVerificationEngine

expect class PlatformJwtVerificationEngine() : JwtVerificationEngine {
    override suspend fun verify(
        token: DecodedJwt,
        publicKey: PublicKey,
        params: TokenValidationParams?
    ): AuthenticationDecision
}
