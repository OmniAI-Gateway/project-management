package org.omniai.sdk.interceptors.auth

import com.nimbusds.jose.jwk.JWKSet

actual fun parseAllKeysFromJson(jwksJson: String): Map<String, Any> {
    val jwkSet = JWKSet.parse(jwksJson)
    val keysMap = mutableMapOf<String, Any>()

    // Itera sobre todas as chaves encontradas no JSON do AS
    jwkSet.keys.forEach { jwk ->
        val kid = jwk.keyID ?: return@forEach
        try {
            // Converte o formato JWK para um objeto PublicKey do Java
            val publicKey = jwk.toPublicJWK().toRSAKey().toPublicKey()
            keysMap[kid] = publicKey
        } catch (e: Exception) {
            // Ignora chaves que não consiga processar (ex: algoritmos não suportados)
        }
    }
    return keysMap
}