package org.omniai.sdk.interceptors.auth.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JwtHeader(
    @SerialName("alg") val algorithm: String, // RFC 7515: Signature algorithm
    @SerialName("kid") val keyId: String? = null, // RFC 7515: Key ID for validation
    @SerialName("typ") val type: String? = null, // Typically "JWT"
)
