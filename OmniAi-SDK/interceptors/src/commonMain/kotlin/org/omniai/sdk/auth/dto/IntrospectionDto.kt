package org.omniai.sdk.auth.dto

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
import org.omniai.sdk.auth.domain.IntrospectionResult

@Serializable
data class IntrospectionDto(
    val active: Boolean,
    val sub: String? = null,
    val scope: String? = null,
    val client_id: String? = null,
    val username: String? = null,
    val exp: Long? = null,
    val iat: Long? = null,
    val iss: String? = null,
    @Serializable(with = AudienceSerializer::class)
    val aud: List<String> = emptyList()
) {

    private object AudienceSerializer : KSerializer<List<String>> {
        override val descriptor: SerialDescriptor = ListSerializer(String.serializer()).descriptor

        override fun deserialize(decoder: Decoder): List<String> {
            val input = decoder as? JsonDecoder ?: throw IllegalStateException("Este serializer só funciona com JSON")
            return when (val element = input.decodeJsonElement()) {
                is JsonArray -> element.map { it.jsonPrimitive.content }
                is JsonPrimitive -> listOf(element.content)
                else -> emptyList()
            }
        }

        override fun serialize(encoder: Encoder, value: List<String>) {
            encoder.encodeSerializableValue(ListSerializer(String.serializer()), value)
        }
    }
    fun toDomain(): IntrospectionResult {
        return IntrospectionResult(
            active = this.active,
            sub = this.sub,
            scope = this.scope,
            clientId = this.client_id,
            username = this.username,
            exp = this.exp,
            iat = this.iat,
            iss = this.iss,
            aud = this.aud
        )
    }
}


