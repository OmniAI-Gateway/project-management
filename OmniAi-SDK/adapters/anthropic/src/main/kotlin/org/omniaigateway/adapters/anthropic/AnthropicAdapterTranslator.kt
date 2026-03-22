package org.omniaigateway.adapters.anthropic

import org.omniaigateway.contracts.anthropic.output.AnthropicMessageResponse
import org.omniaigateway.contracts.anthropic.output.AnthropicOutputContent
import org.omniaigateway.contracts.anthropic.output.AnthropicUsage
import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.content.JsonPart
import org.omniaigateway.domain.common.content.RefusalPart
import org.omniaigateway.domain.common.content.ResponseContentPart
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.common.content.ToolCallPart
import org.omniaigateway.domain.common.json.JsonValue
import org.omniaigateway.domain.common.json.toRawMap
import org.omniaigateway.domain.responses.CommonResponse
import org.omniaigateway.domain.responses.CommonUsage
import org.omniaigateway.domain.responses.FinishReason

class AnthropicAdapterTranslator {
    fun fromDomain(domain: CommonResponse): AnthropicMessageResponse {
        val firstChoice = domain.choices.firstOrNull()

        return AnthropicMessageResponse(
            id = domain.id ?: "",
            type = "message",
            role = firstChoice?.message?.role?.toAnthropicRole() ?: "assistant",
            model = domain.model,
            content = firstChoice?.message?.content?.mapNotNull(::toAnthropicContentPart).orEmpty(),
            stopReason = firstChoice?.finishReason.toAnthropicStopReason(),
            usage = domain.usage?.let(::toAnthropicUsage)
        )
    }
}

private fun toAnthropicContentPart(part: ResponseContentPart): AnthropicOutputContent? =
    when (part) {
        is TextPart -> AnthropicOutputContent.Text(text = part.text)
        is ToolCallPart -> AnthropicOutputContent.ToolUse(
            id = part.toolCallId,
            name = part.functionName,
            input = JsonValue.JsonObject(part.argumentsJson).toRawMap()
        )
        is JsonPart -> null
        is RefusalPart -> null
    }

private fun toAnthropicUsage(usage: CommonUsage): AnthropicUsage = AnthropicUsage(
    inputTokens = usage.inputTokens,
    outputTokens = usage.outputTokens
)

private fun CommonRole.toAnthropicRole(): String =
    when (this) {
        CommonRole.SYSTEM -> "assistant"
        CommonRole.USER -> "user"
        CommonRole.ASSISTANT -> "assistant"
        CommonRole.TOOL -> "assistant"
    }

private fun FinishReason?.toAnthropicStopReason(): String? =
    when (this) {
        FinishReason.STOP -> "end_turn"
        FinishReason.LENGTH -> "max_tokens"
        FinishReason.TOOL_CALL -> "tool_use"
        FinishReason.CONTENT_FILTER -> "stop_sequence"
        FinishReason.OTHER, null -> null
    }


