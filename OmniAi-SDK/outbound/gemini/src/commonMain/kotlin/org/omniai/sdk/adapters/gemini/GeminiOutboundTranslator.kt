package org.omniai.sdk.adapters.gemini

import org.omniai.sdk.contracts.gemini.input.GeminiContent
import org.omniai.sdk.contracts.gemini.input.GeminiFunctionCall as GeminiInputFunctionCall
import org.omniai.sdk.contracts.gemini.input.GeminiFunctionCallingConfig
import org.omniai.sdk.contracts.gemini.input.GeminiFunctionDeclaration
import org.omniai.sdk.contracts.gemini.input.GeminiFunctionResponse
import org.omniai.sdk.contracts.gemini.input.GeminiGenerateContentRequest
import org.omniai.sdk.contracts.gemini.input.GeminiGenerationConfig
import org.omniai.sdk.contracts.gemini.input.GeminiPart
import org.omniai.sdk.contracts.gemini.input.GeminiSystemInstruction
import org.omniai.sdk.contracts.gemini.input.GeminiThinkingConfig
import org.omniai.sdk.contracts.gemini.input.GeminiTool
import org.omniai.sdk.contracts.gemini.input.GeminiToolConfig
import org.omniai.sdk.domain.common.CommonRole
import org.omniai.sdk.domain.common.CommonTool
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.common.SystemPrompt
import org.omniai.sdk.domain.common.ToolChoice
import org.omniai.sdk.domain.common.content.JsonPart
import org.omniai.sdk.domain.common.content.RequestContentPart
import org.omniai.sdk.domain.common.content.ResponseContentPart
import org.omniai.sdk.domain.common.content.TextPart
import org.omniai.sdk.domain.common.content.ToolCallPart
import org.omniai.sdk.domain.common.content.ToolResultPart
import org.omniai.sdk.domain.common.json.JsonValue
import org.omniai.sdk.domain.common.json.toJsonObject
import org.omniai.sdk.domain.common.json.toRawAny
import org.omniai.sdk.domain.common.json.toRawMap
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.requests.CommonRequestMessage
import org.omniai.sdk.domain.responses.ChoiceFinished
import org.omniai.sdk.domain.responses.ChoiceStarted
import org.omniai.sdk.domain.responses.CommonChoice
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent
import org.omniai.sdk.domain.responses.CommonUsage
import org.omniai.sdk.domain.responses.FinishReason
import org.omniai.sdk.domain.responses.ResponseErrored
import org.omniai.sdk.domain.responses.ResponseStarted
import org.omniai.sdk.domain.responses.TextDeltaEvent
import org.omniai.sdk.domain.responses.ToolCallArgumentsDeltaEvent
import org.omniai.sdk.domain.responses.ToolCallStartedEvent
import org.omniai.sdk.domain.responses.UsageReported
import org.omniai.sdk.contracts.gemini.output.GeminiCandidate
import org.omniai.sdk.contracts.gemini.output.GeminiGenerateContentResponse
import org.omniai.sdk.contracts.gemini.output.GeminiResponsePart
import org.omniai.sdk.contracts.gemini.output.GeminiUsageMetadata
import org.omniai.sdk.core.ports.OutboundTranslator
import kotlin.random.Random

class GeminiOutboundTranslator : OutboundTranslator<GeminiGenerateContentRequest, GeminiGenerateContentResponse, GeminiGenerateContentResponse> {

    override fun fromDomain(domainRequest: CommonRequest): GeminiGenerateContentRequest {
        val providerOptions = domainRequest.providerOptions

        return GeminiGenerateContentRequest(
            contents = domainRequest.messages.map(CommonRequestMessage::toGeminiContent),
            systemInstruction = domainRequest.systemPrompt?.toGeminiSystemInstruction(),
            tools = domainRequest.tools.toGeminiTools(),
            toolConfig = domainRequest.toolChoice?.toGeminiToolConfig(),
            generationConfig = GeminiGenerationConfig(
                stopSequences = domainRequest.config?.stopSequences,
                temperature = domainRequest.config?.temperature,
                topP = domainRequest.config?.topP,
                topK = providerOptions["topK"] as? Int,
                thinkingConfig = providerOptions["thinkingConfig"] as? GeminiThinkingConfig,
                responseMimeType = when {
                    domainRequest.jsonResponse -> "application/json"
                    else -> providerOptions["responseMimeType"] as? String
                },
                responseJsonSchema = providerOptions["responseJsonSchema"] as? Map<String, Any?>
            )
        )
    }

    override fun toDomain(providerResponse: GeminiGenerateContentResponse): CommonResponse =
        CommonResponse(
            provider = Provider.GEMINI,
            id = providerResponse.responseId,
            model = providerResponse.modelVersion ?: "",
            choices = providerResponse.candidates.map(::toDomainChoice),
            usage = providerResponse.usageMetadata?.toDomainUsage(),
            providerOptions = providerResponse.promptFeedback?.let { mapOf("promptFeedback" to it.blockReason) } ?: emptyMap()
        )

    override fun toDomainEvent(providerEvent: GeminiGenerateContentResponse): CommonResponseEvent {
        val model = Model(providerEvent.modelVersion ?: "")
        val sequence = 0L
        val firstCandidate = providerEvent.candidates.firstOrNull()

        providerEvent.promptFeedback?.blockReason?.let {
            return ResponseErrored(
                provider = Provider.GEMINI,
                id = providerEvent.responseId,
                model = model,
                sequence = sequence,
                message = it,
                retryable = it.contains("TRANSIENT", ignoreCase = true),
                providerEventType = "prompt_feedback"
            )
        }

        if (providerEvent.candidates.isEmpty()) {
            providerEvent.usageMetadata?.let {
                return UsageReported(
                    provider = Provider.GEMINI,
                    id = providerEvent.responseId,
                    model = model,
                    sequence = sequence,
                    usage = it.toDomainUsage(),
                    providerEventType = "usage"
                )
            }

            return ResponseStarted(
                provider = Provider.GEMINI,
                id = providerEvent.responseId,
                model = model,
                sequence = sequence,
                providerEventType = "response_start"
            )
        }

        val candidate = firstCandidate ?: return ResponseStarted(
            provider = Provider.GEMINI,
            id = providerEvent.responseId,
            model = model,
            sequence = sequence,
            providerEventType = "response_start"
        )

        candidate.finishReason?.let {
            return ChoiceFinished(
                provider = Provider.GEMINI,
                id = providerEvent.responseId,
                model = model,
                sequence = sequence,
                choiceIndex = candidate.index ?: 0,
                finishReason = it.toDomainFinishReason(),
                providerEventType = "choice_finished"
            )
        }

        val functionCall = candidate.content?.parts.orEmpty().firstNotNullOfOrNull { it.functionCall }
        if (functionCall != null) {
            val partialJson = functionCall.args?.get("partialJson") as? String
            if (partialJson != null && functionCall.name.isBlank()) {
                return ToolCallArgumentsDeltaEvent(
                    provider = Provider.GEMINI,
                    id = providerEvent.responseId,
                    model = model,
                    sequence = sequence,
                    choiceIndex = candidate.index ?: 0,
                    toolCallIndex = 0,
                    argumentsFragment = partialJson,
                    providerEventType = "tool_call_arguments_delta"
                )
            }

            return ToolCallStartedEvent(
                provider = Provider.GEMINI,
                id = providerEvent.responseId,
                model = model,
                sequence = sequence,
                choiceIndex = candidate.index ?: 0,
                toolCallIndex = 0,
                toolCallId = functionCall.id ?: "gemini-tool-call-${Random.nextInt(100000, 999999)}",
                functionName = functionCall.name,
                providerEventType = "tool_call_started"
            )
        }

        val textDelta = candidate.content?.parts.orEmpty().firstNotNullOfOrNull { it.text }
        if (textDelta != null) {
            return TextDeltaEvent(
                provider = Provider.GEMINI,
                id = providerEvent.responseId,
                model = model,
                sequence = sequence,
                choiceIndex = candidate.index ?: 0,
                text = textDelta,
                providerEventType = "text_delta"
            )
        }

        candidate.content?.role?.let {
            return ChoiceStarted(
                provider = Provider.GEMINI,
                id = providerEvent.responseId,
                model = model,
                sequence = sequence,
                choiceIndex = candidate.index ?: 0,
                role = it.toCommonRole(),
                providerEventType = "choice_started"
            )
        }

        return ResponseStarted(
            provider = Provider.GEMINI,
            id = providerEvent.responseId,
            model = model,
            sequence = sequence,
            providerEventType = "response_start"
        )
    }

}

private fun CommonRequestMessage.toGeminiContent(): GeminiContent =
    GeminiContent(
        role = role.toGeminiRole(),
        parts = content.mapNotNull { it.toGeminiPart() }
    )

private fun RequestContentPart.toGeminiPart(): GeminiPart? =
    when (this) {
        is TextPart -> GeminiPart(text = text)
        is ToolCallPart -> GeminiPart(
            functionCall = GeminiInputFunctionCall(
                id = toolCallId,
                name = functionName,
                args = JsonValue.JsonObject(argumentsJson).toRawMap()
            )
        )
        is ToolResultPart -> GeminiPart(
            functionResponse = GeminiFunctionResponse(
                name = toolCallId,
                response = content.firstOrNull()?.toRawAny() as? Map<String, Any?> ?: mapOf("value" to content.firstOrNull()?.toRawAny())
            )
        )
        is JsonPart -> GeminiPart(text = json.toString())
    }

private fun SystemPrompt.toGeminiSystemInstruction(): GeminiSystemInstruction =
    GeminiSystemInstruction(parts = listOf(GeminiPart(text = text)))

private fun List<CommonTool>.toGeminiTools(): List<GeminiTool>? {
    if (isEmpty()) return null
    return listOf(
        GeminiTool(
            functionDeclarations = map {
                GeminiFunctionDeclaration(
                    name = it.name,
                    description = it.description,
                    parameters = JsonValue.JsonObject(it.parametersSchema).toRawMap().cleanGeminiParameters()
                )
            }
        )
    )
}

private fun ToolChoice.toGeminiToolConfig(): GeminiToolConfig =
    GeminiToolConfig(
        functionCallingConfig = when (this) {
            ToolChoice.Auto -> GeminiFunctionCallingConfig(mode = "AUTO")
            ToolChoice.None -> GeminiFunctionCallingConfig(mode = "NONE")
            ToolChoice.Required -> GeminiFunctionCallingConfig(mode = "ANY")
            is ToolChoice.Specific -> GeminiFunctionCallingConfig(mode = "ANY", allowedFunctionNames = toolNames)
        }
    )

private fun toDomainChoice(candidate: GeminiCandidate): CommonChoice {
    val parts = candidate.content?.parts.orEmpty().mapNotNull { it.toDomainPart() }
    val hasToolCalls = parts.any { it is ToolCallPart }
    return CommonChoice(
        index = candidate.index ?: 0,
        message = org.omniai.sdk.domain.responses.CommonResponseMessage(
            role = candidate.content?.role.toCommonRole(),
            content = parts
        ),
        finishReason =  if (hasToolCalls) {
            FinishReason.TOOL_CALL
        } else {
            candidate.finishReason.toDomainFinishReason()
        }
    )
}

private fun GeminiResponsePart.toDomainPart(): ResponseContentPart? {
    return when {
        text != null -> TextPart(text ?: return null)
        functionCall != null -> {
            val fc = functionCall ?: return null
            ToolCallPart(
                toolCallId = fc.id ?: "gemini-tool-call-0",
                functionName = fc.name,
                argumentsJson = fc.args.orEmpty().toJsonObject().properties
            )
        }
        else -> null
    }
}

private fun GeminiUsageMetadata.toDomainUsage(): CommonUsage =
    CommonUsage(
        inputTokens = promptTokenCount,
        outputTokens = candidatesTokenCount,
        totalTokens = totalTokenCount
    )

private fun String?.toCommonRole(): CommonRole =
    when (this?.lowercase()) {
        "system" -> CommonRole.SYSTEM
        "model", "assistant" -> CommonRole.ASSISTANT
        "tool", "function" -> CommonRole.TOOL
        "user" -> CommonRole.USER
        else -> CommonRole.USER
    }

private fun String?.toDomainFinishReason(): FinishReason? =
    when (this?.uppercase()) {
        "STOP" -> FinishReason.STOP
        "MAX_TOKENS" -> FinishReason.LENGTH
        "SAFETY" -> FinishReason.CONTENT_FILTER
        null -> null
        else -> FinishReason.OTHER
    }

private fun CommonRole.toGeminiRole(): String =
    when (this) {
        CommonRole.SYSTEM -> "model"
        CommonRole.USER -> "user"
        CommonRole.ASSISTANT -> "model"
        CommonRole.TOOL -> "tool"
    }


// deve ser passada para uma funcao de traducao de apis
private fun Map<*, *>.cleanGeminiParameters(): Map<String, Any?> {
    val keysToRemove = setOf(
        "\$schema",
        "additionalProperties",
        "propertyNames",
        "title",
        "default",
        "\$id"
    )

    val cleaned = mutableMapOf<String, Any?>()

    for ((k, value) in this) {
        val key = k.toString()
        if (key in keysToRemove) continue
        if (key == "const") {
            cleaned["enum"] = listOf(value)
            continue
        }
        val cleanedValue = when (value) {
            is Map<*, *> -> value.cleanGeminiParameters()
            is List<*> -> value.map { item ->
                if (item is Map<*, *>) item.cleanGeminiParameters() else item
            }
            else -> value
        }
        cleaned[key] = cleanedValue
    }

    return cleaned
}