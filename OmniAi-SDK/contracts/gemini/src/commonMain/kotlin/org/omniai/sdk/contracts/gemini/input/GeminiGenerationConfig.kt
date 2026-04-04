package org.omniai.sdk.contracts.gemini.input

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GeminiGenerationConfig(
    val stopSequences: List<String>? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val thinkingConfig: GeminiThinkingConfig? = null,
    val responseMimeType: String? = null,
    val responseJsonSchema: JsonObject? = null
)
