package org.omniaigateway.inbound.web.gemini.mapper

import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.CommonTool
import org.omniaigateway.domain.common.SystemPrompt
import org.omniaigateway.domain.common.ToolChoice
import org.omniaigateway.domain.common.content.RequestContentPart
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.common.content.ToolCallPart
import org.omniaigateway.domain.common.content.ToolResultPart
import org.omniaigateway.domain.common.json.toJsonObject
import org.omniaigateway.domain.common.json.toJsonValue
import org.omniaigateway.domain.responses.FinishReason
import org.omniaigateway.domain.requests.CommonRequestMessage
import org.omniaigateway.inbound.web.gemini.dto.input.GeminiContent
import org.omniaigateway.inbound.web.gemini.dto.input.GeminiFunctionCallingConfig
import org.omniaigateway.inbound.web.gemini.dto.input.GeminiFunctionDeclaration
import org.omniaigateway.inbound.web.gemini.dto.input.GeminiPart
import org.omniaigateway.inbound.web.gemini.dto.input.GeminiSystemInstruction

internal fun String?.toCommonRole(): CommonRole =
    when (this?.lowercase()) {
        "system" -> CommonRole.SYSTEM
        "model", "assistant" -> CommonRole.ASSISTANT
        "tool", "function" -> CommonRole.TOOL
        "user" -> CommonRole.USER
        else -> CommonRole.USER
    }

internal fun GeminiPart.toDomainPart(index: Int): RequestContentPart? =
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

internal fun GeminiContent.toDomainMessage(): CommonRequestMessage =
    CommonRequestMessage(
        role = role.toCommonRole(),
        content = parts.mapIndexedNotNull { index, part -> part.toDomainPart(index) }
    )

internal fun GeminiSystemInstruction.toSystemPrompt(): SystemPrompt? {
    val text = parts.mapNotNull { it.text?.takeIf(String::isNotBlank) }.joinToString("\n")
    return text.takeIf(String::isNotBlank)?.let(::SystemPrompt)
}

internal fun GeminiFunctionDeclaration.toDomainTool(): CommonTool =
    CommonTool(
        name = name,
        description = description,
        parametersSchema = parameters.toJsonObject().properties
    )

internal fun GeminiFunctionCallingConfig.toDomainToolChoice(): ToolChoice? {
    if (!allowedFunctionNames.isNullOrEmpty()) return ToolChoice.Specific(allowedFunctionNames)

    return when (mode.lowercase()) {
        "auto" -> ToolChoice.Auto
        "none" -> ToolChoice.None
        "any", "required" -> ToolChoice.Required
        else -> null
    }
}

internal fun CommonRole.toGeminiRole(): String =
    when (this) {
        CommonRole.SYSTEM -> "model"
        CommonRole.USER -> "user"
        CommonRole.ASSISTANT -> "model"
        CommonRole.TOOL -> "tool"
    }

internal fun FinishReason?.toGeminiFinishReason(): String? =
    when (this) {
        FinishReason.STOP -> "STOP"
        FinishReason.LENGTH -> "MAX_TOKENS"
        FinishReason.TOOL_CALL -> "STOP"
        FinishReason.CONTENT_FILTER -> "SAFETY"
        FinishReason.OTHER, null -> null
    }


