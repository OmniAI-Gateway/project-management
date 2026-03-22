package org.omniaigateway.inbound.web.gemini.dto.input

data class GeminiGenerateContentRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiSystemInstruction? = null,
    val tools: List<GeminiTool>? = null,
    val toolConfig: GeminiToolConfig? = null,
    val generationConfig: GeminiGenerationConfig? = null
)
