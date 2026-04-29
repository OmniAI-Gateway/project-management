package org.omniai.sdk.auth.domain

data class OidcMetadata(
    val issuer: String,
    val jwksUri: String,
    val introspectionEndpoint: String? = null
)