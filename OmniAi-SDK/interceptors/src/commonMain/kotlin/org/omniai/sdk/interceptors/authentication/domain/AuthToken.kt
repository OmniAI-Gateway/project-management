package org.omniai.sdk.interceptors.auth.domain

sealed interface AuthToken

data class JWT(
    val token: DecodedJwt,
) : AuthToken

data class OPAQUE(
    val token: OpaqueToken,
) : AuthToken
