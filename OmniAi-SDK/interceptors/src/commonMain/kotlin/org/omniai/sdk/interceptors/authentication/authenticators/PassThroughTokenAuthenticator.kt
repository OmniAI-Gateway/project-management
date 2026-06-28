package org.omniai.sdk.interceptors.auth.authenticators

import org.omniai.sdk.interceptors.auth.domain.AuthToken
import org.omniai.sdk.interceptors.auth.domain.AuthValidationResult
import org.omniai.sdk.interceptors.auth.domain.AuthenticationDecision
import org.omniai.sdk.interceptors.auth.domain.TokenValidationParams
import org.omniai.sdk.interceptors.auth.interfaces.TokenAuthenticator

class PassThroughTokenAuthenticator : TokenAuthenticator {
    override suspend fun authenticate(
        token: AuthToken,
        params: TokenValidationParams?,
    ): AuthenticationDecision = AuthenticationDecision.Allow(AuthValidationResult.PassThrough(token))
}
