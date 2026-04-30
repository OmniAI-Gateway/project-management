package org.omniai.sdk.auth.domain

import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class DecodedJwt(
    val header: JwtHeader,
    val payload: JwtPayload,
    val signature: String,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun decode(token: String): DecodedJwt {
            val parts = token.split(".")
            require(parts.size == 3) { "Invalid JWT format. Expected 3 parts, got ${parts.size}" }

            val headerJson = parts[0].base64UrlDecode()
            val header = json.decodeFromString<JwtHeader>(headerJson)

            val payloadJson = parts[1].base64UrlDecode()
            val payload = json.decodeFromString<JwtPayload>(payloadJson)

            return DecodedJwt(header, payload, parts[2])
        }

        @OptIn(ExperimentalEncodingApi::class)
        private fun String.base64UrlDecode(): String {
            val padded = this.padEnd(this.length + (4 - this.length % 4) % 4, '=')
            return Base64.UrlSafe
                .decode(padded)
                .decodeToString()
        }
    }
}
