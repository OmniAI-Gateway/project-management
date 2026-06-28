package org.omniai.sdk.interceptors.auth.interfaces

import org.omniai.sdk.interceptors.auth.domain.AuthenticationDecision
import org.omniai.sdk.interceptors.auth.domain.DecodedJwt
import org.omniai.sdk.interceptors.auth.domain.PublicKey
import org.omniai.sdk.interceptors.auth.domain.TokenValidationParams

interface JwtVerificationEngine {
    suspend fun verify(
        token: DecodedJwt,
        publicKey: PublicKey,
        params: TokenValidationParams?
    ): AuthenticationDecision
}