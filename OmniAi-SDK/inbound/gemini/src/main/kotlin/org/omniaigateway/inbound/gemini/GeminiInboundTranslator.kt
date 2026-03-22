package org.omniaigateway.inbound.gemini

import org.omniaigateway.core.ports.InboundTranslator
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
import org.omniaigateway.contracts.gemini.input.GeminiContent
import org.omniaigateway.contracts.gemini.input.GeminiFunctionCallingConfig
import org.omniaigateway.contracts.gemini.input.GeminiFunctionDeclaration
import org.omniaigateway.contracts.gemini.input.GeminiGenerateContentRequest
import org.omniaigateway.contracts.gemini.input.GeminiPart
import org.omniaigateway.contracts.gemini.input.GeminiSystemInstruction

class GeminiInboundTranslator : InboundTranslator<GeminiGenerateContentRequest> {
    override val provider: Provider = Provider.GEMINI

    override fun toDomain(payload: GeminiGenerateContentRequest): CommonRequest {
        val providerOptions = buildMap<String, Any?> {
            payload.generationConfig?.topK?.let { put("topK", it) }
            payload.generationConfig?.thinkingConfig?.let { put("thinkingConfig", it) }
            payload.generationConfig?.responseMimeType?.let { put("responseMimeType", it) }
            payload.generationConfig?.responseJsonSchema?.let { put("responseJsonSchema", it) }
        }

        return CommonRequest(
            provider = provider,
            model = payload.model,
            messages = payload.contents.map { it.toDomainMessage() },
            systemPrompt = payload.systemInstruction?.toSystemPrompt(),
            config = CommonGenerationConfig(
                temperature = payload.generationConfig?.temperature,
                topP = payload.generationConfig?.topP,
                stopSequences = payload.generationConfig?.stopSequences
            ),
            tools = payload.tools.orEmpty().flatMap { tool ->
                tool.functionDeclarations.orEmpty().map { declaration -> declaration.toDomainTool() }
            },
            toolChoice = payload.toolConfig?.functionCallingConfig?.toDomainToolChoice(),
            jsonResponse = payload.generationConfig?.responseMimeType.equals("application/json", ignoreCase = true)
                || payload.generationConfig?.responseJsonSchema != null,
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
        text != null -> TextPart(text = text.orEmpty())
        functionCall != null -> {
            val call = functionCall ?: return null
            ToolCallPart(
                toolCallId = call.id ?: "gemini-tool-call-$index",
                functionName = call.name,
                argumentsJson = call.args.orEmpty().toJsonObject().properties
            )
        }
        functionResponse != null -> {
            val response = functionResponse ?: return null
            ToolResultPart(
                toolCallId = response.name,
                content = listOf(response.response.toJsonValue())
            )
        }
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
    val allowed = allowedFunctionNames
    if (!allowed.isNullOrEmpty()) return ToolChoice.Specific(allowed)

    return when (mode.lowercase()) {
        "auto" -> ToolChoice.Auto
        "none" -> ToolChoice.None
        "any", "required" -> ToolChoice.Required
        else -> null
    }
}


