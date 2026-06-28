package org.omniai.sdk.interceptors.auth.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.omniai.sdk.interceptors.auth.domain.IntrospectionResult

/**
 * DTO representing the Token Introspection response.
 * Compliant with RFC 7662 (OAuth 2.0 Token Introspection).
 */
@Serializable
data class IntrospectionDto(
    // REQUIRED by RFC 7662: Boolean indicator of whether or not the presented token is currently active.
    val active: Boolean,
    // OPTIONAL by RFC 7662: Subject of the token (usually a machine-readable identifier of the resource owner).
    val sub: String? = null,
    // OPTIONAL by RFC 7662: A JSON string containing a space-separated list of scopes associated with this token.
    val scope: String? = null,
    // OPTIONAL by RFC 7662: Client identifier for the OAuth 2.0 client that requested this token.
    val client_id: String? = null,
    // OPTIONAL by RFC 7662: Human-readable identifier for the resource owner who authorized this token.
    val username: String? = null,
    // OPTIONAL by RFC 7662: Integer timestamp indicating when this token will expire (seconds since Epoch).
    val exp: Long? = null,
    // OPTIONAL by RFC 7662: Integer timestamp indicating when this token was originally issued.
    val iat: Long? = null,
    // OPTIONAL by RFC 7662: String representing the issuer of this token.
    val iss: String? = null,
    // OPTIONAL by RFC 7662: Audience for this token.
    // Handled safely here as it can be a single string or an array of strings.
    @Serializable(with = AudienceSerializer::class)
    val aud: List<String> = emptyList(),
) {
    private object AudienceSerializer : KSerializer<List<String>> {
        override val descriptor: SerialDescriptor = ListSerializer(String.serializer()).descriptor

        override fun deserialize(decoder: Decoder): List<String> {
            val input = decoder as? JsonDecoder ?: throw IllegalStateException("This serializer only works with JSON")
            return when (val element = input.decodeJsonElement()) {
                is JsonArray -> element.map { it.jsonPrimitive.content }
                is JsonPrimitive -> listOf(element.content)
                else -> emptyList()
            }
        }

        override fun serialize(
            encoder: Encoder,
            value: List<String>,
        ) {
            encoder.encodeSerializableValue(ListSerializer(String.serializer()), value)
        }
    }

    fun toDomain(): IntrospectionResult =
        IntrospectionResult(
            active = this.active,
            sub = this.sub,
            scope = this.scope,
            clientId = this.client_id,
            username = this.username,
            exp = this.exp,
            iat = this.iat,
            iss = this.iss,
            aud = this.aud,
        )
}
