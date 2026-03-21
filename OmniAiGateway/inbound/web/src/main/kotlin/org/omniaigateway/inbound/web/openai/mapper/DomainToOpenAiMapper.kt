package org.omniaigateway.inbound.web.openai.mapper

import org.omniaigateway.domain.common.content.JsonPart
import org.omniaigateway.domain.common.content.RefusalPart
import org.omniaigateway.domain.common.content.ResponseContentPart
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.common.content.ToolCallPart
import org.omniaigateway.domain.common.json.JsonValue
import org.omniaigateway.domain.common.json.toRawAny
import org.omniaigateway.domain.common.json.toRawMap
import org.omniaigateway.domain.responses.CommonChoice
import org.omniaigateway.domain.responses.CommonResponse
import org.omniaigateway.domain.responses.CommonUsage
import org.omniaigateway.inbound.web.openai.dto.output.OpenAiChatCompletionsResponse
import org.omniaigateway.inbound.web.openai.dto.output.OpenAiChoice
import org.omniaigateway.inbound.web.openai.dto.output.OpenAiMessageOutput
import org.omniaigateway.inbound.web.openai.dto.output.OpenAiToolCallFunctionOutput
import org.omniaigateway.inbound.web.openai.dto.output.OpenAiToolCallOutput
import org.omniaigateway.inbound.web.openai.dto.output.OpenAiUsage

fun CommonResponse.toOpenAiChatCompletionsResponse(): OpenAiChatCompletionsResponse =
    OpenAiChatCompletionsResponse(
        id = id ?: "",
        `object` = "chat.completion",
        created = ((providerOptions["created"] as? Number)?.toLong() ?: (System.currentTimeMillis() / 1000)),
        model = model,
        systemFingerprint = providerOptions["systemFingerprint"] as? String,
        choices = choices.map(::toOpenAiChoice),
        usage = usage?.let(::toOpenAiUsage)
    )

private fun toOpenAiChoice(choice: CommonChoice): OpenAiChoice =
    OpenAiChoice(
        index = choice.index,
        message = OpenAiMessageOutput(
            role = choice.message.role.toOpenAiRole(),
            content = choice.message.content.firstTextOrFallback(),
            toolCalls = choice.message.content.mapNotNull(::toOpenAiToolCall).ifEmpty { null }
        ),
        finishReason = choice.finishReason.toOpenAiFinishReason()
    )

private fun List<ResponseContentPart>.firstTextOrFallback(): String? {
    val text = filterIsInstance<TextPart>().joinToString(separator = "\n") { it.text }
    if (text.isNotBlank()) return text

    val refusal = firstOrNull { it is RefusalPart } as? RefusalPart
    if (refusal != null) return refusal.reason

    val jsonPart = firstOrNull { it is JsonPart } as? JsonPart
    return jsonPart?.json?.toRawAny()?.toString()
}

private fun toOpenAiToolCall(part: ResponseContentPart): OpenAiToolCallOutput? =
    when (part) {
        is ToolCallPart -> OpenAiToolCallOutput(
            id = part.toolCallId,
            type = "function",
            function = OpenAiToolCallFunctionOutput(
                name = part.functionName,
                arguments = JsonValue.JsonObject(part.argumentsJson).toRawMap()
            )
        )
        is TextPart, is JsonPart, is RefusalPart -> null
    }

private fun toOpenAiUsage(usage: CommonUsage): OpenAiUsage =
    OpenAiUsage(
        totalTokens = usage.totalTokens,
        completionTokens = usage.outputTokens,
        promptTokens = usage.inputTokens
    )

