package org.omniaigateway.inbound.web.openai.mapper

import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.CommonTool
import org.omniaigateway.domain.common.ToolChoice
import org.omniaigateway.domain.common.content.RequestContentPart
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.common.content.ToolCallPart
import org.omniaigateway.domain.common.content.ToolResultPart
import org.omniaigateway.domain.common.json.JsonValue
import org.omniaigateway.domain.common.json.toJsonObject
import org.omniaigateway.domain.common.json.toJsonValue
import org.omniaigateway.domain.responses.FinishReason
import org.omniaigateway.domain.requests.CommonRequestMessage
import org.omniaigateway.inbound.web.openai.dto.input.OpenAiMessageInput
import org.omniaigateway.inbound.web.openai.dto.input.OpenAiTool
import org.omniaigateway.inbound.web.openai.dto.input.OpenAiToolCall
import org.omniaigateway.inbound.web.openai.dto.input.OpenAiToolChoice

internal fun String.toCommonRole(): CommonRole =
    when (lowercase()) {
        "system" -> CommonRole.SYSTEM
        "assistant" -> CommonRole.ASSISTANT
        "tool" -> CommonRole.TOOL
        "user" -> CommonRole.USER
        else -> CommonRole.USER
    }

internal fun OpenAiToolCall.toDomainPart(index: Int): ToolCallPart =
    ToolCallPart(
        toolCallId = id ?: "openai-tool-call-$index",
        functionName = function.name,
        argumentsJson = mapOf("raw" to JsonValue.JsonString(function.arguments))
    )

internal fun OpenAiMessageInput.toDomainMessage(): CommonRequestMessage =
    CommonRequestMessage(
        role = role.toCommonRole(),
        content = buildList<RequestContentPart> {
            content?.takeIf { it.isNotBlank() }?.let { add(TextPart(it)) }
            toolCalls.orEmpty().forEachIndexed { index, toolCall -> add(toolCall.toDomainPart(index)) }
            if (role.equals("tool", ignoreCase = true) && toolCallId != null && content != null) {
                add(ToolResultPart(toolCallId = toolCallId, content = listOf(content.toJsonValue())))
            }
        }
    )

internal fun OpenAiTool.toDomainTool(): CommonTool =
    CommonTool(
        name = function.name,
        description = function.description ?: function.name,
        parametersSchema = function.parameters.orEmpty().toJsonObject().properties
    )

internal fun OpenAiToolChoice.toDomainToolChoice(): ToolChoice? =
    when (this) {
        is OpenAiToolChoice.Mode -> value.toToolChoice()
        is OpenAiToolChoice.Function -> ToolChoice.Specific(function.name)
    }

internal fun String.toToolChoice(): ToolChoice? =
    when (lowercase()) {
        "auto" -> ToolChoice.Auto
        "none" -> ToolChoice.None
        "required" -> ToolChoice.Required
        else -> null
    }

internal fun CommonRole.toOpenAiRole(): String =
    when (this) {
        CommonRole.SYSTEM -> "system"
        CommonRole.USER -> "user"
        CommonRole.ASSISTANT -> "assistant"
        CommonRole.TOOL -> "tool"
    }

internal fun FinishReason?.toOpenAiFinishReason(): String? =
    when (this) {
        FinishReason.STOP -> "stop"
        FinishReason.LENGTH -> "length"
        FinishReason.TOOL_CALL -> "tool_calls"
        FinishReason.CONTENT_FILTER -> "content_filter"
        FinishReason.OTHER, null -> null
    }

