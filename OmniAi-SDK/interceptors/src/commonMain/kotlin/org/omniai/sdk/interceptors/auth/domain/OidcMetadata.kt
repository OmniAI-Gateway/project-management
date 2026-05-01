package org.omniai.sdk.interceptors.auth.domain

data class OidcMetadata(
    val issuer: String,
    val jwksUri: String,
    val introspectionEndpoint: String? = null
)