package org.omniai.sdk.auth.domain

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

data class IntrospectionResult(
    val active: Boolean,
    val sub: String? = null,
    val scope: String? = null,
    val clientId: String? = null,
    val username: String? = null,
    val exp: Long? = null,
    val iat: Long? = null,
    val iss: String? = null,
    val aud: List<String> = emptyList()
)
