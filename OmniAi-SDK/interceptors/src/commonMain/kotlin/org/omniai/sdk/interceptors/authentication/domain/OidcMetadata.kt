package org.omniai.sdk.interceptors.auth.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class OidcMetadata(
    val issuer: String,

    @SerialName("jwks_uri")
    val jwksUri: String,

    @SerialName("introspection_endpoint")
    val introspectionEndpoint: String? = null
)