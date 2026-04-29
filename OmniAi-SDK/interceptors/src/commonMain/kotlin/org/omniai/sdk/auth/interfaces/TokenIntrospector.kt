package org.omniai.sdk.auth.interfaces

import org.omniai.sdk.auth.domain.IntrospectionResult

interface TokenIntrospector {
    suspend fun introspectToken(token: String): IntrospectionResult?
}

