package org.omniai.sdk.auth

import org.omniai.sdk.auth.domain.AuthToken
import org.omniai.sdk.auth.domain.AuthenticationDecision
import org.omniai.sdk.auth.domain.TokenValidationParams
import org.omniai.sdk.auth.interfaces.TokenAuthenticator

class PassThroughTokenAuthenticator : TokenAuthenticator {
    override suspend fun authenticate(token: AuthToken, params: TokenValidationParams): AuthenticationDecision =
        AuthenticationDecision.Allow(claims = emptyMap())
}
