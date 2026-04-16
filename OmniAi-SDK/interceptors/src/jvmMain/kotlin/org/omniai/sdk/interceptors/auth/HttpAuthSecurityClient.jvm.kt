package org.omniai.sdk.interceptors.auth

import com.nimbusds.jose.jwk.JWKSet

actual fun parsePublicKeyFromJson(jwksJson: String, keyId: String?): Any {
    val jwkSet = JWKSet.parse(jwksJson)

    return if (keyId != null) {
        // O Nimbus procura a chave específica pelo ID (kid)
        jwkSet.getKeyByKeyId(keyId)
            ?: throw RuntimeException("Chave com ID '$keyId' não encontrada no servidor de Auth")
    } else {
        // Se não houver kid no token, pegamos a primeira chave disponível
        jwkSet.keys.firstOrNull()
            ?: throw RuntimeException("O servidor de Auth não devolveu nenhuma chave válida")
    }
}