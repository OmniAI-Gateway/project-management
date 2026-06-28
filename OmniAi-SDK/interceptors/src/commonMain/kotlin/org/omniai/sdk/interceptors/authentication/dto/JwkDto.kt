package org.omniai.sdk.interceptors.auth.dto

import kotlinx.serialization.Serializable
import org.omniai.sdk.interceptors.auth.domain.Base64
import org.omniai.sdk.interceptors.auth.domain.Kid
import org.omniai.sdk.interceptors.auth.domain.PublicKey
import org.omniai.sdk.interceptors.auth.utils.generateRsaPublicKeyBase64

/**
 * DTO representing a single JSON Web Key (JWK).
 * Compliant with RFC 7517 (JSON Web Key).
 */
@Serializable
data class JwkDto(
    // REQUIRED by RFC 7517 (when multiple keys exist): Key ID used to match a specific key.
    val kid: String,
    // REQUIRED by RFC 7517: Key Type (e.g., "RSA" or "EC"). Identifies the cryptographic algorithm family.
    val kty: String,
    // OPTIONAL by RFC 7517: Algorithm intended for use with the key (e.g., "RS256").
    val alg: String? = null,
    // OPTIONAL by RFC 7517: Public Key Use (e.g., "sig" for signature or "enc" for encryption).
    val use: String? = null,
    // --- RSA-specific parameters (RFC 7518) ---
    // The modulus value for the RSA public key.
    val n: String? = null,
    // The exponent value for the RSA public key.
    val e: String? = null,
    // --- Elliptic Curve (EC) specific parameters (RFC 7518) ---
    // The cryptographic curve used with the key.
    val crv: String? = null,
    // The x coordinate for the Elliptic Curve point.
    val x: String? = null,
    // The y coordinate for the Elliptic Curve point.
    val y: String? = null,
) {
    fun toDomain(): Pair<Kid, PublicKey>? {
        return when (kty) {
            "RSA" -> {
                if (n == null || e == null) return null

                val base64Key = generateRsaPublicKeyBase64(n, e) ?: return null

                Kid(kid) to
                    PublicKey(
                        key = Base64(base64Key),
                        algorithm = "RSA",
                    )
            }

            "EC" -> {
                println("Warning: EC (Elliptic Curve) key type detected, but not yet supported.")
                null
            }

            else -> {
                println("Warning: Unknown or unsupported key type: $kty")
                null
            }
        }
    }
}
