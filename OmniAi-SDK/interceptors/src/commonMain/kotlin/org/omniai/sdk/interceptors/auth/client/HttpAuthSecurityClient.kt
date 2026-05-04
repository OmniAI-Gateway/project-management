package org.omniai.sdk.interceptors.auth.client

import org.omniai.sdk.interceptors.auth.domain.IntrospectionResult
import org.omniai.sdk.interceptors.auth.domain.Kid
import org.omniai.sdk.interceptors.auth.domain.OpaqueToken
import org.omniai.sdk.interceptors.auth.domain.PublicKey
import org.omniai.sdk.interceptors.auth.interfaces.AuthSecurityInfrastructure

/**
 * Facade implementation of [AuthSecurityInfrastructure] that delegates to focused sub-clients:
 * - [JwksClient] — public key management and JWKS rotation
 * - [HttpIntrospectionClient] — opaque token introspection with caching
 *
 * Introspection is optional: if no [introspectionClient] is provided, [introspectToken] returns null,
 * which causes opaque tokens to be rejected by the authenticator.
 */
class HttpAuthSecurityClient(
    private val jwksClient: JwksClient,
    private val introspectionClient: HttpIntrospectionClient?,
) : AuthSecurityInfrastructure {

    override suspend fun getPublicKey(keyId: Kid): PublicKey? =
        jwksClient.getPublicKey(keyId)

    override suspend fun introspectToken(token: OpaqueToken): IntrospectionResult? =
        introspectionClient?.introspect(token)
}
