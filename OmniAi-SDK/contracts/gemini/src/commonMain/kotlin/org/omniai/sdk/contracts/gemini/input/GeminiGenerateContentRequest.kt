package org.omniai.sdk.contracts.gemini.input

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiGenerateContentRequest(
    val contents: List<GeminiContent>,
    @SerialName("system_instruction")
    val systemInstruction: GeminiSystemInstruction? = null,
    val tools: List<GeminiTool>? = null,
    val toolConfig: GeminiToolConfig? = null,
    val generationConfig: GeminiGenerationConfig? = null,
    val model: String? = null
) {
    fun injectModel(model: String) = copy(model = model)
}
