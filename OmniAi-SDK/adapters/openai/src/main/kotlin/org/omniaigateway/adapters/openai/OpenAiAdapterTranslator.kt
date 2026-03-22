package org.omniaigateway.adapters.openai

import org.omniaigateway.contracts.openai.output.OpenAiChatCompletionsResponse
import org.omniaigateway.contracts.openai.output.OpenAiChoice
import org.omniaigateway.contracts.openai.output.OpenAiMessageOutput
import org.omniaigateway.contracts.openai.output.OpenAiToolCallFunctionOutput
import org.omniaigateway.contracts.openai.output.OpenAiToolCallOutput
import org.omniaigateway.contracts.openai.output.OpenAiUsage
<<<<<<<< HEAD:OmniAi-SDK/adapters/openai/src/main/kotlin/org/omniaigateway/adapters/openai/OpenAiAdapterTranslator.kt
========
import org.omniaigateway.core.ports.AdapterTranslator
>>>>>>>> origin/main:OmniAi-SDK/adapters/openai/src/main/kotlin/org/omniaigateway/adapters/openai/OpenAiResponseTranslator.kt
import org.omniaigateway.domain.common.CommonRole
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
import org.omniaigateway.domain.responses.FinishReason

<<<<<<<< HEAD:OmniAi-SDK/adapters/openai/src/main/kotlin/org/omniaigateway/adapters/openai/OpenAiAdapterTranslator.kt
class OpenAiAdapterTranslator {
    fun fromDomain(domain: CommonResponse): OpenAiChatCompletionsResponse =
========
class OpenAiResponseTranslator : AdapterTranslator<CommonResponse, OpenAiChatCompletionsResponse> {
    override fun fromDomain(domain: CommonResponse): OpenAiChatCompletionsResponse =
>>>>>>>> origin/main:OmniAi-SDK/adapters/openai/src/main/kotlin/org/omniaigateway/adapters/openai/OpenAiResponseTranslator.kt
        OpenAiChatCompletionsResponse(
            id = domain.id ?: "",
            `object` = "chat.completion",
            created = ((domain.providerOptions["created"] as? Number)?.toLong() ?: (System.currentTimeMillis() / 1000)),
            model = domain.model,
            systemFingerprint = domain.providerOptions["systemFingerprint"] as? String,
            choices = domain.choices.map(::toOpenAiChoice),
            usage = domain.usage?.let(::toOpenAiUsage)
        )
}

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

private fun CommonRole.toOpenAiRole(): String =
    when (this) {
        CommonRole.SYSTEM -> "system"
        CommonRole.USER -> "user"
        CommonRole.ASSISTANT -> "assistant"
        CommonRole.TOOL -> "tool"
    }

private fun FinishReason?.toOpenAiFinishReason(): String? =
    when (this) {
        FinishReason.STOP -> "stop"
        FinishReason.LENGTH -> "length"
        FinishReason.TOOL_CALL -> "tool_calls"
        FinishReason.CONTENT_FILTER -> "content_filter"
        FinishReason.OTHER, null -> null
    }

<<<<<<<< HEAD:OmniAi-SDK/adapters/openai/src/main/kotlin/org/omniaigateway/adapters/openai/OpenAiAdapterTranslator.kt

========
>>>>>>>> origin/main:OmniAi-SDK/adapters/openai/src/main/kotlin/org/omniaigateway/adapters/openai/OpenAiResponseTranslator.kt
