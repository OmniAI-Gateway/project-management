package org.omniai.sdk.auth.interfaces

import org.omniai.sdk.auth.domain.AuthenticationDecision
import org.omniai.sdk.auth.domain.DecodedJwt
import org.omniai.sdk.auth.domain.PublicKey
import org.omniai.sdk.auth.domain.TokenValidationParams

interface JwtVerificationEngine {
    suspend fun verify(
        token: DecodedJwt,
        publicKey: PublicKey,
        params: TokenValidationParams?
    ): AuthenticationDecision
}