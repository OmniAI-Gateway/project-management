package org.omniai.sdk.interceptors.auth.utils

import java.math.BigInteger
import java.security.KeyFactory
import java.security.spec.RSAPublicKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
actual fun generateRsaPublicKeyBase64(modulusBase64Url: String, exponentBase64Url: String): String? {
    return try {
        val nBytes = Base64.UrlSafe.decode(modulusBase64Url)
        val eBytes = Base64.UrlSafe.decode(exponentBase64Url)

        val modulus = BigInteger(1, nBytes)
        val exponent = BigInteger(1, eBytes)

        val spec = RSAPublicKeySpec(modulus, exponent)
        val factory = KeyFactory.getInstance("RSA")
        val publicKey = factory.generatePublic(spec)

        Base64.encode(publicKey.encoded)
    } catch (e: Exception) {
        println("Erro ao gerar a chave RSA: ${e.message}")
        null
    }
}
