package org.omniaigateway.adapters.gemini

import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.content.JsonPart
import org.omniaigateway.domain.common.content.RefusalPart
import org.omniaigateway.domain.common.content.ResponseContentPart
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.common.content.ToolCallPart
import org.omniaigateway.domain.common.json.JsonValue
import org.omniaigateway.domain.common.json.toRawMap
import org.omniaigateway.domain.responses.CommonChoice
import org.omniaigateway.domain.responses.CommonResponse
import org.omniaigateway.domain.responses.CommonUsage
import org.omniaigateway.domain.responses.FinishReason
import org.omniaigateway.contracts.gemini.output.GeminiCandidate
import org.omniaigateway.contracts.gemini.output.GeminiFunctionCall
import org.omniaigateway.contracts.gemini.output.GeminiGenerateContentResponse
import org.omniaigateway.contracts.gemini.output.GeminiResponseContent
import org.omniaigateway.contracts.gemini.output.GeminiResponsePart
import org.omniaigateway.contracts.gemini.output.GeminiUsageMetadata

class GeminiAdapterTranslator {
    fun fromDomain(domain: CommonResponse): GeminiGenerateContentResponse =
        GeminiGenerateContentResponse(
            candidates = domain.choices.map(::toGeminiCandidate),
            usageMetadata = domain.usage?.let(::toGeminiUsageMetadata),
            modelVersion = domain.model,
            responseId = domain.id
        )
}

private fun toGeminiCandidate(choice: CommonChoice): GeminiCandidate =
    GeminiCandidate(
        content = GeminiResponseContent(
            parts = choice.message.content.map(::toGeminiPart),
            role = choice.message.role.toGeminiRole()
        ),
        finishReason = choice.finishReason.toGeminiFinishReason(),
        index = choice.index
    )

private fun toGeminiPart(part: ResponseContentPart): GeminiResponsePart =
    when (part) {
        is TextPart -> GeminiResponsePart(text = part.text)
        is ToolCallPart -> GeminiResponsePart(
            functionCall = GeminiFunctionCall(
                name = part.functionName,
                args = JsonValue.JsonObject(part.argumentsJson).toRawMap(),
                id = part.toolCallId
            )
        )
        is JsonPart -> GeminiResponsePart(text = part.json.toString())
        is RefusalPart -> GeminiResponsePart(text = part.reason)
    }

private fun toGeminiUsageMetadata(usage: CommonUsage): GeminiUsageMetadata =
    GeminiUsageMetadata(
        promptTokenCount = usage.inputTokens,
        candidatesTokenCount = usage.outputTokens,
        totalTokenCount = usage.totalTokens
    )

private fun CommonRole.toGeminiRole(): String =
    when (this) {
        CommonRole.SYSTEM -> "model"
        CommonRole.USER -> "user"
        CommonRole.ASSISTANT -> "model"
        CommonRole.TOOL -> "tool"
    }

private fun FinishReason?.toGeminiFinishReason(): String? =
    when (this) {
        FinishReason.STOP -> "STOP"
        FinishReason.LENGTH -> "MAX_TOKENS"
        FinishReason.TOOL_CALL -> "STOP"
        FinishReason.CONTENT_FILTER -> "SAFETY"
        FinishReason.OTHER, null -> null
    }


