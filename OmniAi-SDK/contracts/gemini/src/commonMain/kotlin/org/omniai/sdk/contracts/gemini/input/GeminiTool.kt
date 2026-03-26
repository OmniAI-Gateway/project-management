package org.omniai.sdk.contracts.gemini.input

import kotlinx.serialization.Serializable
import org.omniai.sdk.contracts.gemini.serialization.NullableStringAnyMapSerializer

@Serializable
data class GeminiTool(
    val functionDeclarations: List<GeminiFunctionDeclaration>? = null,
    @Serializable(with = NullableStringAnyMapSerializer::class)
    val googleSearch: Map<String, Any?>? = null,
    @Serializable(with = NullableStringAnyMapSerializer::class)
    val urlContext: Map<String, Any?>? = null
)

