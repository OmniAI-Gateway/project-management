package org.omniai.sdk.interceptors.auth

import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.domain.common.AUTH_OPAQUE_CLAIMS_KEY

data class OpaqueIntrospectionClaims(
    val subject: String,
    val clientId: String,
    val values: Map<String, String> = emptyMap()
) {
    fun asAttributeMap(): Map<String, String> = values + mapOf(
        "sub" to subject,
        "client_id" to clientId
    )
}

sealed interface OpaqueIntrospectionResult {
    data class Active(val claims: OpaqueIntrospectionClaims) : OpaqueIntrospectionResult
    data class Inactive(val reason: String) : OpaqueIntrospectionResult
}

fun interface OpaqueTokenIntrospector {
    suspend fun introspect(rawToken: String): OpaqueIntrospectionResult
}

class StubOpaqueTokenIntrospector : OpaqueTokenIntrospector {
    override suspend fun introspect(rawToken: String): OpaqueIntrospectionResult {
        return OpaqueIntrospectionResult.Inactive("Opaque token introspection not configured")
    }
}

class OpaqueAwareTokenAuthenticator(
    private val jwtAuthenticator: TokenAuthenticator,
    private val opaqueTokenIntrospector: OpaqueTokenIntrospector,
    private val opaqueIntrospectionEnabled: Boolean
) : TokenAuthenticator {

    override suspend fun authenticate(token: AuthToken, context: GatewayContext): AuthenticationDecision {
        if (token.kind == TokenKind.JWT) {
            return jwtAuthenticator.authenticate(token, context)
        }

        if (!opaqueIntrospectionEnabled) {
            return AuthenticationDecision.Allow
        }

        return when (val introspection = opaqueTokenIntrospector.introspect(token.rawValue)) {
            is OpaqueIntrospectionResult.Active -> {
                context.attributes.put(AUTH_OPAQUE_CLAIMS_KEY, introspection.claims.asAttributeMap())
                AuthenticationDecision.Allow
            }

            is OpaqueIntrospectionResult.Inactive -> {
                AuthenticationDecision.Deny("Opaque token is inactive: ${introspection.reason}")
            }
        }
    }
}

