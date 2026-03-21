package org.omniaigateway.inbound.web.gemini.dto.input

import org.omniaigateway.domain.common.CommonGenerationConfig
import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.CommonTool
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.common.SystemPrompt
import org.omniaigateway.domain.common.ToolChoice
import org.omniaigateway.domain.common.content.RequestContentPart
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.common.content.ToolCallPart
import org.omniaigateway.domain.common.content.ToolResultPart
import org.omniaigateway.domain.common.json.toJsonObject
import org.omniaigateway.domain.common.json.toJsonValue
import org.omniaigateway.domain.requests.CommonRequest
import org.omniaigateway.domain.requests.CommonRequestMessage
import org.omniaigateway.inbound.web.DomainMappableIn

data class GeminiGenerateContentRequest(
    val model: String,
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiSystemInstruction? = null,
    val tools: List<GeminiTool>? = null,
    val toolConfig: GeminiToolConfig? = null,
    val generationConfig: GeminiGenerationConfig? = null
) : DomainMappableIn<CommonRequest> {
    override fun toDomain(): CommonRequest {
        val providerOptions = buildMap<String, Any?> {
            generationConfig?.topK?.let { put("topK", it) }
            generationConfig?.thinkingConfig?.let { put("thinkingConfig", it) }
            generationConfig?.responseMimeType?.let { put("responseMimeType", it) }
            generationConfig?.responseJsonSchema?.let { put("responseJsonSchema", it) }
        }

        return CommonRequest(
            provider = Provider.GEMINI,
            model = model,
            messages = contents.map { it.toDomainMessage() },
            systemPrompt = systemInstruction?.toSystemPrompt(),
            config = CommonGenerationConfig(
                temperature = generationConfig?.temperature,
                topP = generationConfig?.topP,
                stopSequences = generationConfig?.stopSequences
            ),
            tools = tools.orEmpty().flatMap { tool ->
                tool.functionDeclarations.orEmpty().map { declaration -> declaration.toDomainTool() }
            },
            toolChoice = toolConfig?.functionCallingConfig?.toDomainToolChoice(),
            jsonResponse = generationConfig?.responseMimeType.equals("application/json", ignoreCase = true)
                || generationConfig?.responseJsonSchema != null,
            providerOptions = providerOptions
        )
    }
}

private fun String?.toCommonRole(): CommonRole =
    when (this?.lowercase()) {
        "system" -> CommonRole.SYSTEM
        "model", "assistant" -> CommonRole.ASSISTANT
        "tool", "function" -> CommonRole.TOOL
        "user" -> CommonRole.USER
        else -> CommonRole.USER
    }

private fun GeminiPart.toDomainPart(index: Int): RequestContentPart? =
    when {
        text != null -> TextPart(text)
        functionCall != null -> ToolCallPart(
            toolCallId = functionCall.id ?: "gemini-tool-call-$index",
            functionName = functionCall.name,
            argumentsJson = functionCall.args.orEmpty().toJsonObject().properties
        )
        functionResponse != null -> ToolResultPart(
            toolCallId = functionResponse.name,
            content = listOf(functionResponse.response.toJsonValue())
        )
        else -> null
    }

private fun GeminiContent.toDomainMessage(): CommonRequestMessage =
    CommonRequestMessage(
        role = role.toCommonRole(),
        content = parts.mapIndexedNotNull { index, part -> part.toDomainPart(index) }
    )

private fun GeminiSystemInstruction.toSystemPrompt(): SystemPrompt? {
    val text = parts.mapNotNull { it.text?.takeIf(String::isNotBlank) }.joinToString("\n")
    return text.takeIf(String::isNotBlank)?.let(::SystemPrompt)
}

private fun GeminiFunctionDeclaration.toDomainTool(): CommonTool =
    CommonTool(
        name = name,
        description = description,
        parametersSchema = parameters.toJsonObject().properties
    )

private fun GeminiFunctionCallingConfig.toDomainToolChoice(): ToolChoice? {
    if (!allowedFunctionNames.isNullOrEmpty()) return ToolChoice.Specific(allowedFunctionNames)

    return when (mode.lowercase()) {
        "auto" -> ToolChoice.Auto
        "none" -> ToolChoice.None
        "any", "required" -> ToolChoice.Required
        else -> null
    }
}
