package org.omniaigateway.inbound.web.anthropic.mapper

import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.ToolChoice
import org.omniaigateway.domain.common.content.RequestContentPart
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.common.content.ToolCallPart
import org.omniaigateway.domain.common.content.ToolResultPart
import org.omniaigateway.domain.common.json.toJsonObject
import org.omniaigateway.domain.common.json.toJsonValue
import org.omniaigateway.domain.responses.FinishReason
import org.omniaigateway.inbound.web.anthropic.dto.input.AnthropicContent
import org.omniaigateway.inbound.web.anthropic.dto.input.AnthropicInputContentBlock
import org.omniaigateway.inbound.web.anthropic.dto.input.AnthropicMessageInput
import org.omniaigateway.inbound.web.anthropic.dto.input.AnthropicToolChoice
import org.omniaigateway.inbound.web.anthropic.dto.input.AnthropicToolDefinition
import org.omniaigateway.inbound.web.anthropic.dto.input.ListContentBlock
import org.omniaigateway.inbound.web.anthropic.dto.input.RawText
import org.omniaigateway.domain.common.CommonTool
import org.omniaigateway.domain.requests.CommonRequestMessage

internal fun String.toCommonRole(): CommonRole =
    when (lowercase()) {
        "user" -> CommonRole.USER
        "assistant" -> CommonRole.ASSISTANT
        "tool" -> CommonRole.TOOL
        "system" -> CommonRole.SYSTEM
        else -> CommonRole.USER
    }

internal fun AnthropicInputContentBlock.toDomainPart(index: Int): RequestContentPart? =
    when (this) {
        is AnthropicInputContentBlock.Text -> TextPart(text)
        is AnthropicInputContentBlock.ToolUse -> ToolCallPart(
            toolCallId = id ?: "anthropic-tool-use-$index",
            functionName = name,
            argumentsJson = input.orEmpty().toJsonObject().properties
        )
        is AnthropicInputContentBlock.ToolResult -> ToolResultPart(
            toolCallId = toolUseId,
            content = listOf(content.toJsonValue())
        )
        is AnthropicInputContentBlock.Thinking -> null
    }

internal fun AnthropicContent.toDomainParts(): List<RequestContentPart> =
    when (this) {
        is RawText -> listOf(TextPart(text))
        is ListContentBlock -> blocks.mapIndexedNotNull { index, block -> block.toDomainPart(index) }
    }

internal fun AnthropicMessageInput.toDomainMessage(): CommonRequestMessage =
    CommonRequestMessage(
        role = role.toCommonRole(),
        content = content.toDomainParts()
    )

internal fun AnthropicToolDefinition.toDomainTool(): CommonTool =
    CommonTool(
        name = name,
        description = description,
        parametersSchema = inputSchema.toJsonObject().properties
    )

internal fun AnthropicToolChoice.toDomainToolChoice(): ToolChoice? =
    type.toToolChoice(name)

internal fun String.toToolChoice(name: String?): ToolChoice? =
    when (lowercase()) {
        "auto" -> ToolChoice.Auto
        "none" -> ToolChoice.None
        "any" -> ToolChoice.Required
        "tool" -> name?.let(ToolChoice::Specific)
        else -> null
    }

internal fun CommonRole.toAnthropicRole(): String =
    when (this) {
        CommonRole.SYSTEM -> "assistant"
        CommonRole.USER -> "user"
        CommonRole.ASSISTANT -> "assistant"
        CommonRole.TOOL -> "assistant"
    }

internal fun FinishReason?.toAnthropicStopReason(): String? =
    when (this) {
        FinishReason.STOP -> "end_turn"
        FinishReason.LENGTH -> "max_tokens"
        FinishReason.TOOL_CALL -> "tool_use"
        FinishReason.CONTENT_FILTER -> "stop_sequence"
        FinishReason.OTHER, null -> null
    }

