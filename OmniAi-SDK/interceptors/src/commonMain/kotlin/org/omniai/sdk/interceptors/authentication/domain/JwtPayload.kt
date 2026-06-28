package org.omniai.sdk.interceptors.auth.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

@Serializable(with = JwtPayload.JwtPayloadSerializer::class)
data class JwtPayload(
    // --- Registered Claims (RFC 7519 Section 4.1) ---
    // "iss" (Issuer) Claim: Identifies the principal that issued the JWT.
    val issuer: String? = null,
    // "sub" (Subject) Claim: Identifies the principal that is the subject of the JWT.
    val subject: String? = null,
    // "aud" (Audience) Claim: Identifies the recipients that the JWT is intended for.
    // Per RFC 7519 §4.1.3, this can be a single StringOrURI or an array of StringOrURI values.
    val audience: List<String>? = null,
    // "exp" (Expiration Time) Claim: Identifies the expiration time on or after which the JWT MUST NOT be accepted.
    val expirationTime: Long? = null,
    // "nbf" (Not Before) Claim: Identifies the time before which the JWT MUST NOT be accepted.
    val notBefore: Long? = null,
    // "iat" (Issued At) Claim: Identifies the time at which the JWT was issued.
    val issuedAt: Long? = null,
    // "jti" (JWT ID) Claim: Provides a unique identifier for the JWT.
    val jwtId: String? = null,
    // --- Private/Custom Claims (RFC 7519 Section 4.3) ---
    // Acts as a catch-all map for any claim not officially registered in the RFC.
    val privateClaims: Map<String, JsonElement> = emptyMap(),
) {
    internal object JwtPayloadSerializer : KSerializer<JwtPayload> {
        override val descriptor = buildClassSerialDescriptor("JwtPayload")

        override fun deserialize(decoder: Decoder): JwtPayload {
            val jsonDecoder = decoder as? JsonDecoder ?: error("Only supports JSON")
            val jsonObject = jsonDecoder.decodeJsonElement().jsonObject

            // RFC 7519 registered claims
            val registeredClaims = setOf("iss", "sub", "aud", "exp", "nbf", "iat", "jti")

            val iss = jsonObject["iss"]?.jsonPrimitive?.contentOrNull
            val sub = jsonObject["sub"]?.jsonPrimitive?.contentOrNull

            // RFC 7519
            val aud: List<String>? =
                jsonObject["aud"]?.let { element ->
                    when (element) {
                        is JsonArray -> element.mapNotNull { it.jsonPrimitive.contentOrNull }
                        is JsonPrimitive -> element.contentOrNull?.let { listOf(it) }
                        else -> null
                    }
                }
            val exp = jsonObject["exp"]?.jsonPrimitive?.longOrNull
            val nbf = jsonObject["nbf"]?.jsonPrimitive?.longOrNull
            val iat = jsonObject["iat"]?.jsonPrimitive?.longOrNull
            val jti = jsonObject["jti"]?.jsonPrimitive?.contentOrNull
            val privateClaims = jsonObject.filterKeys { key -> key !in registeredClaims }

            return JwtPayload(iss, sub, aud, exp, nbf, iat, jti, privateClaims)
        }

        override fun serialize(
            encoder: Encoder,
            value: JwtPayload,
        ) {
            val jsonEncoder = encoder as? JsonEncoder ?: error("Only supports JSON")

            val jsonObject =
                buildJsonObject {
                    value.issuer?.let { put("iss", it) }
                    value.subject?.let { put("sub", it) }
                    value.audience?.let { aud ->
                        when {
                            aud.size == 1 -> put("aud", aud.single())
                            aud.size > 1 -> put("aud", buildJsonArray { aud.forEach { add(it) } })
                        }
                    }
                    value.expirationTime?.let { put("exp", it) }
                    value.notBefore?.let { put("nbf", it) }
                    value.issuedAt?.let { put("iat", it) }
                    value.jwtId?.let { put("jti", it) }
                    value.privateClaims.forEach { (key, element) ->
                        put(key, element)
                    }
                }
            jsonEncoder.encodeJsonElement(jsonObject)
        }
    }
}
