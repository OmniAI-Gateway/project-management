package org.omniai.sdk.auth.interfaces

import org.omniai.sdk.auth.domain.AuthToken
import org.omniai.sdk.auth.domain.AuthenticationDecision
import org.omniai.sdk.auth.domain.TokenValidationParams
import org.omniai.sdk.core.pipeline.GatewayContext

interface TokenAuthenticator {
    suspend fun authenticate(token: AuthToken, params: TokenValidationParams): AuthenticationDecision
}