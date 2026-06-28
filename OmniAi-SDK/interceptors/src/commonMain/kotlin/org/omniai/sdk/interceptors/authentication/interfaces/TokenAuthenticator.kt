package org.omniai.sdk.interceptors.auth.interfaces

import org.omniai.sdk.interceptors.auth.domain.AuthToken
import org.omniai.sdk.interceptors.auth.domain.AuthenticationDecision
import org.omniai.sdk.interceptors.auth.domain.TokenValidationParams

interface TokenAuthenticator {
    suspend fun authenticate(
        token: AuthToken,
        params: TokenValidationParams?,
    ): AuthenticationDecision
}
