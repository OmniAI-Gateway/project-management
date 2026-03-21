package org.omniaigateway.inbound.web.gemini.mapper

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
import org.omniaigateway.inbound.web.gemini.dto.output.GeminiCandidate
import org.omniaigateway.inbound.web.gemini.dto.output.GeminiFunctionCall
import org.omniaigateway.inbound.web.gemini.dto.output.GeminiGenerateContentResponse
import org.omniaigateway.inbound.web.gemini.dto.output.GeminiResponseContent
import org.omniaigateway.inbound.web.gemini.dto.output.GeminiResponsePart
import org.omniaigateway.inbound.web.gemini.dto.output.GeminiUsageMetadata

fun CommonResponse.toGeminiGenerateContentResponse(): GeminiGenerateContentResponse =
    GeminiGenerateContentResponse(
        candidates = choices.map(::toGeminiCandidate),
        usageMetadata = usage?.let(::toGeminiUsageMetadata),
        modelVersion = model,
        responseId = id
    )

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

