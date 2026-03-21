package org.omniaigateway.inbound.web.anthropic.mapper

import org.omniaigateway.domain.common.content.ResponseContentPart
import org.omniaigateway.domain.common.content.JsonPart
import org.omniaigateway.domain.common.content.RefusalPart
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.common.content.ToolCallPart
import org.omniaigateway.domain.common.json.JsonValue
import org.omniaigateway.domain.common.json.toRawMap
import org.omniaigateway.domain.responses.CommonResponse
import org.omniaigateway.domain.responses.CommonUsage
import org.omniaigateway.inbound.web.anthropic.dto.output.AnthropicOutputContent
import org.omniaigateway.inbound.web.anthropic.dto.output.AnthropicMessageResponse
import org.omniaigateway.inbound.web.anthropic.dto.output.AnthropicUsage

fun CommonResponse.toAnthropicMessage(): AnthropicMessageResponse {
    val firstChoice = choices.firstOrNull()

    return AnthropicMessageResponse(
        id = id ?: "",
        type = "message",
        role = firstChoice?.message?.role?.toAnthropicRole() ?: "assistant",
        model = model,
        content = firstChoice?.message?.content?.mapNotNull(::toAnthropicContentPart).orEmpty(),
        stopReason = firstChoice?.finishReason.toAnthropicStopReason(),
        usage = usage?.let(::toAnthropicUsage)
    )
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

