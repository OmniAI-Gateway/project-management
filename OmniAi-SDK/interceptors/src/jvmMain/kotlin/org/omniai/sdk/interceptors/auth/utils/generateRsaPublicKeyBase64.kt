package org.omniai.sdk.interceptors.auth.utils

import java.math.BigInteger
import java.security.KeyFactory
import java.security.spec.RSAPublicKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
actual fun generateRsaPublicKeyBase64(
    modulusBase64Url: String,
    exponentBase64Url: String,
): String? {
    // RFC 7517 §2: JWK Base64Url values MUST omit padding ('=') characters.
    // Keycloak and all standard OIDC providers emit unpadded Base64Url for 'n' and 'e'.
    val base64UrlNoPadding = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
    return try {
        val nBytes = base64UrlNoPadding.decode(modulusBase64Url)
        val eBytes = base64UrlNoPadding.decode(exponentBase64Url)

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
