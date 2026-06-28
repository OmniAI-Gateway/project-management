package org.omniai.sdk.interceptors.auth.dto

import kotlinx.serialization.Serializable

/**
 * DTO representing a JSON Web Key Set (JWKS).
 * Compliant with RFC 7517 Section 5 (JWK Set Format).
 */
@Serializable
data class JwksDto(
    // REQUIRED by RFC 7517: An array of JWK values.
    val keys: List<JwkDto>,
)
