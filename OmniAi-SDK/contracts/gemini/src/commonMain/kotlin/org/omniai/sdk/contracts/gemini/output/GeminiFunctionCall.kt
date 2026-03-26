package org.omniai.sdk.contracts.gemini.output

import kotlinx.serialization.Serializable
import org.omniai.sdk.contracts.gemini.serialization.NullableStringAnyMapSerializer

@Serializable
data class GeminiFunctionCall(
    val name: String,
    @Serializable(with = NullableStringAnyMapSerializer::class)
    val args: Map<String, Any?>? = null,
    val id: String? = null
)
