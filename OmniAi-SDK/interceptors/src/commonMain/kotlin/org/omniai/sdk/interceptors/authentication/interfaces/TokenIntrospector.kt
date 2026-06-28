package org.omniai.sdk.interceptors.auth.interfaces

import org.omniai.sdk.interceptors.auth.domain.IntrospectionResult
import org.omniai.sdk.interceptors.auth.domain.OpaqueToken

interface TokenIntrospector {
    suspend fun introspectToken(token: OpaqueToken): IntrospectionResult?
}

