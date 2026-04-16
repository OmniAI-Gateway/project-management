package org.omniai.sdk.interceptors.auth

actual fun parsePublicKeyFromJson(jwksJson: String, keyId: String?): Any {
    // 1. Converte a String JSON num objeto dinâmico do JS
    val json = JSON.parse<dynamic>(jwksJson)
    val keys = json.keys as Array<dynamic>

    // 2. Procura a chave pelo 'kid'
    val targetKey = if (keyId != null) {
        keys.find { it.kid == keyId }
    } else {
        keys.firstOrNull()
    }

    return targetKey ?: throw RuntimeException("Chave pública não encontrada no JWKS (JS)")
}