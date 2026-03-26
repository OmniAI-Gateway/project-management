package org.omniai.sdk.contracts.gemini.input

import kotlinx.serialization.Serializable
import org.omniai.sdk.contracts.gemini.serialization.StringAnyMapSerializer

@Serializable
data class GeminiFunctionResponse(
    val name: String,
    @Serializable(with = StringAnyMapSerializer::class)
    val response: Map<String, Any?>
)
