package org.omniai.sdk.contracts.gemini.input

import kotlinx.serialization.Serializable
import org.omniai.sdk.contracts.gemini.serialization.NullableStringAnyMapSerializer

@Serializable
data class GeminiGenerationConfig(
    val stopSequences: List<String>? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val thinkingConfig: GeminiThinkingConfig? = null,
    val responseMimeType: String? = null,
    @Serializable(with = NullableStringAnyMapSerializer::class)
    val responseJsonSchema: Map<String, Any?>? = null
)
