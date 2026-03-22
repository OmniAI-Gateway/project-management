package org.omniaigateway.contracts.gemini.input

data class GeminiGenerateContentRequest(
    val model: String,
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiSystemInstruction? = null,
    val tools: List<GeminiTool>? = null,
    val toolConfig: GeminiToolConfig? = null,
    val generationConfig: GeminiGenerationConfig? = null
)
