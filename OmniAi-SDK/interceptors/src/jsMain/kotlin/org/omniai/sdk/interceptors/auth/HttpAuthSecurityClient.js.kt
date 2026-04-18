package org.omniai.sdk.interceptors.auth

actual fun parseAllKeysFromJson(jwksJson: String): Map<String, Any> {
    val json = JSON.parse<dynamic>(jwksJson)
    val keysMap = mutableMapOf<String, Any>()
    if (json.keys != null) {
        val keysArray = json.keys as Array<dynamic>
        keysArray.forEach { key ->
            val kid = key.kid as? String ?: return@forEach
            // No JS, guardamos o objeto da chave para ser usado pelo motor de verificação
            keysMap[kid] = key
        }
    }
    return keysMap
}