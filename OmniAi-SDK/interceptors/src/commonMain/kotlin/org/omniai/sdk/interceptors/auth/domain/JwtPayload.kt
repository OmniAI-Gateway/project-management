package org.omniai.sdk.interceptors.auth.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * DTO representing the payload of a JSON Web Token (JWT).
 * Compliant with RFC 7519 (JSON Web Token).
 */
@Serializable(with = JwtPayload.JwtPayloadSerializer::class)
data class JwtPayload(
    // --- Registered Claims (RFC 7519 Section 4.1) ---

    // "iss" (Issuer) Claim: Identifies the principal that issued the JWT.
    val issuer: String? = null,
    // "sub" (Subject) Claim: Identifies the principal that is the subject of the JWT.
    val subject: String? = null,
    // "aud" (Audience) Claim: Identifies the recipients that the JWT is intended for.
    val audience: String? = null,
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
    val privateClaims: Map<String, JsonElement> = emptyMap()
) {
    internal object JwtPayloadSerializer : KSerializer<JwtPayload> {

        override val descriptor = buildClassSerialDescriptor("JwtPayload")

        override fun deserialize(decoder: Decoder): JwtPayload {
            val jsonDecoder = decoder as? JsonDecoder ?: error("Only supports JSON")
            val jsonObject = jsonDecoder.decodeJsonElement().jsonObject

            // Standard RFC 7519 registered claims
            val registeredClaims = setOf("iss", "sub", "aud", "exp", "nbf", "iat", "jti")

            val iss = jsonObject["iss"]?.jsonPrimitive?.contentOrNull
            val sub = jsonObject["sub"]?.jsonPrimitive?.contentOrNull
            val aud = jsonObject["aud"]?.jsonPrimitive?.contentOrNull
            val exp = jsonObject["exp"]?.jsonPrimitive?.longOrNull
            val nbf = jsonObject["nbf"]?.jsonPrimitive?.longOrNull
            val iat = jsonObject["iat"]?.jsonPrimitive?.longOrNull
            val jti = jsonObject["jti"]?.jsonPrimitive?.contentOrNull

            // Filter out the standard claims to isolate the private ones
            val privateClaims = jsonObject.filterKeys { key -> key !in registeredClaims }

            return JwtPayload(iss, sub, aud, exp, nbf, iat, jti, privateClaims)
        }

        override fun serialize(encoder: Encoder, value: JwtPayload) {
            val jsonEncoder = encoder as? JsonEncoder ?: error("Only supports JSON")

            val jsonObject = buildJsonObject {
                value.issuer?.let { put("iss", it) }
                value.subject?.let { put("sub", it) }
                value.audience?.let { put("aud", it) }
                value.expirationTime?.let { put("exp", it) }
                value.notBefore?.let { put("nbf", it) }
                value.issuedAt?.let { put("iat", it) }
                value.jwtId?.let { put("jti", it) }

                // Inject private claims back into the root level of the JSON payload
                value.privateClaims.forEach { (key, element) ->
                    put(key, element)
                }
            }
            jsonEncoder.encodeJsonElement(jsonObject)
        }
    }
}