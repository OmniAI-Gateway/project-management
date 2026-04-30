package org.omniai.sdk.auth.interfaces

import org.omniai.sdk.auth.domain.AuthToken
import org.omniai.sdk.auth.domain.AuthenticationDecision
import org.omniai.sdk.auth.domain.TokenValidationParams

interface TokenAuthenticator {
    suspend fun authenticate(token: AuthToken, params: TokenValidationParams?): AuthenticationDecision
}