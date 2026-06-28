package org.omniai.sdk.interceptors.auth.client

import org.omniai.sdk.interceptors.auth.domain.IntrospectionResult
import org.omniai.sdk.interceptors.auth.domain.Kid
import org.omniai.sdk.interceptors.auth.domain.OpaqueToken
import org.omniai.sdk.interceptors.auth.domain.PublicKey
import org.omniai.sdk.interceptors.auth.interfaces.AuthSecurityInfrastructure

class HttpAuthSecurityClient(
    private val jwksClient: JwksClient,
    private val introspectionClient: HttpIntrospectionClient?,
) : AuthSecurityInfrastructure {
    override suspend fun getPublicKey(keyId: Kid): PublicKey? = jwksClient.getPublicKey(keyId)

    override suspend fun introspectToken(token: OpaqueToken): IntrospectionResult? = introspectionClient?.introspect(token)
}
