package org.omniai.sdk.auth.interfaces

import org.omniai.sdk.auth.domain.IntrospectionResult
import org.omniai.sdk.auth.domain.OpaqueToken

interface TokenIntrospector {
    suspend fun introspectToken(token: OpaqueToken): IntrospectionResult?
}

