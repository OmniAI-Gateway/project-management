package org.omniaigateway.adapters.gemini

import org.omniaigateway.contracts.gemini.input.GeminiContent
import org.omniaigateway.contracts.gemini.input.GeminiFunctionCall as GeminiInputFunctionCall
import org.omniaigateway.contracts.gemini.input.GeminiFunctionCallingConfig
import org.omniaigateway.contracts.gemini.input.GeminiFunctionDeclaration
import org.omniaigateway.contracts.gemini.input.GeminiFunctionResponse
import org.omniaigateway.contracts.gemini.input.GeminiGenerateContentRequest
import org.omniaigateway.contracts.gemini.input.GeminiGenerationConfig
import org.omniaigateway.contracts.gemini.input.GeminiPart
import org.omniaigateway.contracts.gemini.input.GeminiSystemInstruction
import org.omniaigateway.contracts.gemini.input.GeminiTool
import org.omniaigateway.contracts.gemini.input.GeminiToolConfig
import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.CommonTool
import org.omniaigateway.domain.common.Model
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.common.SystemPrompt
import org.omniaigateway.domain.common.ToolChoice
import org.omniaigateway.domain.common.content.JsonPart
import org.omniaigateway.domain.common.content.RequestContentPart
import org.omniaigateway.domain.common.content.ResponseContentPart
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.common.content.ToolCallPart
import org.omniaigateway.domain.common.content.ToolResultPart
import org.omniaigateway.domain.common.json.JsonValue
import org.omniaigateway.domain.common.json.toJsonObject
import org.omniaigateway.domain.common.json.toRawAny
import org.omniaigateway.domain.common.json.toRawMap
import org.omniaigateway.domain.requests.CommonRequest
import org.omniaigateway.domain.requests.CommonRequestMessage
import org.omniaigateway.domain.responses.ChoiceFinished
import org.omniaigateway.domain.responses.ChoiceStarted
import org.omniaigateway.domain.responses.CommonChoice
import org.omniaigateway.domain.responses.CommonResponse
import org.omniaigateway.domain.responses.CommonResponseEvent
import org.omniaigateway.domain.responses.CommonUsage
import org.omniaigateway.domain.responses.FinishReason
import org.omniaigateway.domain.responses.ResponseErrored
import org.omniaigateway.domain.responses.ResponseStarted
import org.omniaigateway.domain.responses.TextDeltaEvent
import org.omniaigateway.domain.responses.ToolCallArgumentsDeltaEvent
import org.omniaigateway.domain.responses.ToolCallStartedEvent
import org.omniaigateway.domain.responses.UsageReported
import org.omniaigateway.contracts.gemini.output.GeminiCandidate
import org.omniaigateway.contracts.gemini.output.GeminiGenerateContentResponse
import org.omniaigateway.contracts.gemini.output.GeminiResponsePart
import org.omniaigateway.contracts.gemini.output.GeminiUsageMetadata
import org.omniaigateway.core.ports.AdapterTranslator

class GeminiAdapterTranslator : AdapterTranslator<GeminiGenerateContentRequest, GeminiGenerateContentResponse, GeminiGenerateContentResponse> {

    override fun fromDomain(domainRequest: CommonRequest): GeminiGenerateContentRequest {
        val providerOptions = domainRequest.providerOptions

        return GeminiGenerateContentRequest(
            model = domainRequest.model,
            contents = domainRequest.messages.map(CommonRequestMessage::toGeminiContent),
            systemInstruction = domainRequest.systemPrompt?.toGeminiSystemInstruction(),
            tools = domainRequest.tools.toGeminiTools(),
            toolConfig = domainRequest.toolChoice?.toGeminiToolConfig(),
            generationConfig = GeminiGenerationConfig(
                stopSequences = domainRequest.config?.stopSequences,
                temperature = domainRequest.config?.temperature,
                topP = domainRequest.config?.topP,
                topK = providerOptions["topK"] as? Int,
                thinkingConfig = providerOptions["thinkingConfig"] as? org.omniaigateway.contracts.gemini.input.GeminiThinkingConfig,
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
                toolCallId = functionCall.id ?: "gemini-tool-call-0",
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
                    parameters = JsonValue.JsonObject(it.parametersSchema).toRawMap()
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

    return CommonChoice(
        index = candidate.index ?: 0,
        message = org.omniaigateway.domain.responses.CommonResponseMessage(
            role = candidate.content?.role.toCommonRole(),
            content = parts
        ),
        finishReason = candidate.finishReason.toDomainFinishReason()
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
