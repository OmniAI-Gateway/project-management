package org.omniai.sdk.contracts.gemini.input

data class GeminiGenerationConfig(
    val stopSequences: List<String>? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val thinkingConfig: GeminiThinkingConfig? = null,
    val responseMimeType: String? = null,
    val responseJsonSchema: Map<String, Any?>? = null
)
