package org.omniai.sdk.auth.dto

import kotlinx.serialization.Serializable
import org.omniai.sdk.auth.utils.generateRsaPublicKeyBase64
import org.omniai.sdk.auth.domain.Kid
import org.omniai.sdk.auth.domain.PublicKey


@Serializable
data class JwkDto(
    val kid: String,
    val kty: String,
    val alg: String? = null,
    val use: String? = null,
    val n: String? = null,
    val e: String? = null,
    val crv: String? = null,
    val x: String? = null,
    val y: String? = null
) {
    fun toDomain(): Pair<Kid, PublicKey>? {
        return when (kty) {
            "RSA" -> {
                if (n == null || e == null) return null

                val base64Key = generateRsaPublicKeyBase64(n, e) ?: return null

                Kid(kid) to PublicKey(
                    key = base64Key,
                    algorithm = "RSA"
                )
            }
            "EC" -> {
                println("Aviso: Chave do tipo EC (Elliptic Curve) detetada, mas ainda não suportada.")
                null
            }
            else -> {
                println("Aviso: Tipo de chave desconhecido ou não suportado: $kty")
                null
            }
        }
    }
}
