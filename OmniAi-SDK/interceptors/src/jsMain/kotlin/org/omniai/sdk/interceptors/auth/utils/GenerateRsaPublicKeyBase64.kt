package org.omniai.sdk.interceptors.auth.utils

actual fun generateRsaPublicKeyBase64(
    modulusBase64Url: String,
    exponentBase64Url: String,
): String? {
    println("Aviso: Geração de chave RSA nativa para iOS ainda não implementada.")
    return null
}
