package org.omniai.sdk.inbound.gemini

import org.omniai.sdk.contracts.gemini.input.GeminiContent
import org.omniai.sdk.contracts.gemini.input.GeminiFunctionCallingConfig
import org.omniai.sdk.contracts.gemini.input.GeminiFunctionDeclaration
import org.omniai.sdk.contracts.gemini.input.GeminiGenerateContentRequest
import org.omniai.sdk.contracts.gemini.input.GeminiPart
import org.omniai.sdk.contracts.gemini.input.GeminiSystemInstruction
import org.omniai.sdk.contracts.gemini.output.GeminiCandidate
import org.omniai.sdk.contracts.gemini.output.GeminiFunctionCall
import org.omniai.sdk.contracts.gemini.output.GeminiGenerateContentResponse
import org.omniai.sdk.contracts.gemini.output.GeminiPromptFeedback
import org.omniai.sdk.contracts.gemini.output.GeminiResponseContent
import org.omniai.sdk.contracts.gemini.output.GeminiResponsePart
import org.omniai.sdk.contracts.gemini.output.GeminiUsageMetadata
import org.omniai.sdk.core.ports.InboundTranslator
import org.omniai.sdk.domain.common.CommonGenerationConfig
import org.omniai.sdk.domain.common.CommonRole
import org.omniai.sdk.domain.common.CommonTool
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.common.SystemPrompt
import org.omniai.sdk.domain.common.ToolChoice
import org.omniai.sdk.domain.common.content.JsonPart
import org.omniai.sdk.domain.common.content.RequestContentPart
import org.omniai.sdk.domain.common.content.RefusalPart
import org.omniai.sdk.domain.common.content.ResponseContentPart
import org.omniai.sdk.domain.common.content.TextPart
import org.omniai.sdk.domain.common.content.ToolCallPart
import org.omniai.sdk.domain.common.content.ToolResultPart
import org.omniai.sdk.domain.common.json.toJsonObject
import org.omniai.sdk.domain.common.json.toRawMap
import org.omniai.sdk.domain.common.json.toJsonValue
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.requests.CommonRequestMessage
import org.omniai.sdk.domain.responses.ChoiceFinished
import org.omniai.sdk.domain.responses.ChoiceStarted
import org.omniai.sdk.domain.responses.CommonChoice
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent
import org.omniai.sdk.domain.responses.CommonUsage
import org.omniai.sdk.domain.responses.FinishReason
import org.omniai.sdk.domain.responses.ResponseCompleted
import org.omniai.sdk.domain.responses.ResponseErrored
import org.omniai.sdk.domain.responses.ResponseStarted
import org.omniai.sdk.domain.responses.TextDeltaEvent
import org.omniai.sdk.domain.responses.ToolCallArgumentsDeltaEvent
import org.omniai.sdk.domain.responses.ToolCallStartedEvent
import org.omniai.sdk.domain.responses.UsageReported

class GeminiInboundTranslator :
    InboundTranslator<GeminiGenerateContentRequest, GeminiGenerateContentResponse, GeminiGenerateContentResponse> {
    override val provider: Provider = Provider.GEMINI

    override fun toDomain(clientRequest: GeminiGenerateContentRequest): CommonRequest {
        val providerOptions = buildMap<String, Any?> {
            clientRequest.generationConfig?.topK?.let { put("topK", it) }
            clientRequest.generationConfig?.thinkingConfig?.let { put("thinkingConfig", it) }
            clientRequest.generationConfig?.responseMimeType?.let { put("responseMimeType", it) }
            clientRequest.generationConfig?.responseJsonSchema?.let { put("responseJsonSchema", it) }
        }

        return CommonRequest(
            provider = provider,
            model = "NO MODEL",
            messages = clientRequest.contents.map { it.toDomainMessage() },
            systemPrompt = clientRequest.systemInstruction?.toSystemPrompt(),
            config = CommonGenerationConfig(
                temperature = clientRequest.generationConfig?.temperature,
                topP = clientRequest.generationConfig?.topP,
                stopSequences = clientRequest.generationConfig?.stopSequences
            ),
            tools = clientRequest.tools.orEmpty().flatMap { tool ->
                tool.functionDeclarations.orEmpty().map { declaration -> declaration.toDomainTool() }
            },
            toolChoice = clientRequest.toolConfig?.functionCallingConfig?.toDomainToolChoice(),
            jsonResponse = clientRequest.generationConfig?.responseMimeType.equals("application/json", ignoreCase = true)
                || clientRequest.generationConfig?.responseJsonSchema != null,
            providerOptions = providerOptions
        )
    }

    override fun fromDomain(domainResponse: CommonResponse): GeminiGenerateContentResponse =
        GeminiGenerateContentResponse(
            candidates = domainResponse.choices.map(::toGeminiCandidate),
            usageMetadata = domainResponse.usage?.toGeminiUsageMetadata(),
            modelVersion = domainResponse.model,
            responseId = domainResponse.id
        )

    override fun fromDomainEvent(domainEvent: CommonResponseEvent): GeminiGenerateContentResponse =
        when (domainEvent) {
            is ResponseStarted -> GeminiGenerateContentResponse(
                modelVersion = domainEvent.model.model,
                responseId = domainEvent.id
            )
            is ChoiceStarted -> GeminiGenerateContentResponse(
                candidates = listOf(
                    GeminiCandidate(
                        index = domainEvent.choiceIndex,
                        content = GeminiResponseContent(role = domainEvent.role?.toGeminiRole() ?: "model")
                    )
                ),
                modelVersion = domainEvent.model.model,
                responseId = domainEvent.id
            )
            is TextDeltaEvent -> GeminiGenerateContentResponse(
                candidates = listOf(
                    GeminiCandidate(
                        index = domainEvent.choiceIndex,
                        content = GeminiResponseContent(parts = listOf(GeminiResponsePart(text = domainEvent.text)))
                    )
                ),
                modelVersion = domainEvent.model.model,
                responseId = domainEvent.id
            )
            is ToolCallStartedEvent -> GeminiGenerateContentResponse(
                candidates = listOf(
                    GeminiCandidate(
                        index = domainEvent.choiceIndex,
                        content = GeminiResponseContent(
                            parts = listOf(
                                GeminiResponsePart(
                                    functionCall = GeminiFunctionCall(
                                        id = domainEvent.toolCallId,
                                        name = domainEvent.functionName,
                                        args = emptyMap()
                                    )
                                )
                            )
                        )
                    )
                ),
                modelVersion = domainEvent.model.model,
                responseId = domainEvent.id
            )
            is ToolCallArgumentsDeltaEvent -> GeminiGenerateContentResponse(
                candidates = listOf(
                    GeminiCandidate(
                        index = domainEvent.choiceIndex,
                        content = GeminiResponseContent(
                            parts = listOf(
                                GeminiResponsePart(
                                    functionCall = GeminiFunctionCall(
                                        id = null,
                                        name = "",
                                        args = mapOf("partialJson" to domainEvent.argumentsFragment)
                                    )
                                )
                            )
                        )
                    )
                ),
                modelVersion = domainEvent.model.model,
                responseId = domainEvent.id
            )
            is ChoiceFinished -> GeminiGenerateContentResponse(
                candidates = listOf(
                    GeminiCandidate(
                        index = domainEvent.choiceIndex,
                        finishReason = domainEvent.finishReason.toGeminiFinishReason()
                    )
                ),
                modelVersion = domainEvent.model.model,
                responseId = domainEvent.id
            )
            is UsageReported -> GeminiGenerateContentResponse(
                usageMetadata = domainEvent.usage.toGeminiUsageMetadata(),
                modelVersion = domainEvent.model.model,
                responseId = domainEvent.id
            )
            is ResponseCompleted -> GeminiGenerateContentResponse(
                modelVersion = domainEvent.model.model,
                responseId = domainEvent.id
            )
            is ResponseErrored -> GeminiGenerateContentResponse(
                promptFeedback = GeminiPromptFeedback(
                    blockReason = if (domainEvent.retryable) {
                        "TRANSIENT_ERROR: ${domainEvent.message}"
                    } else {
                        "ERROR: ${domainEvent.message}"
                    }
                ),
                modelVersion = domainEvent.model.model,
                responseId = domainEvent.id
            )
        }
}

private fun String?.toCommonRole(): CommonRole =
    when (this?.lowercase()) {
        "system" -> CommonRole.SYSTEM
        "model", "assistant" -> CommonRole.ASSISTANT
        "tool", "function" -> CommonRole.TOOL
        "user" -> CommonRole.USER
        else -> CommonRole.USER
    }

private fun GeminiPart.toDomainPart(index: Int): RequestContentPart? =
    when {
        text != null -> TextPart(text = text.orEmpty())
        functionCall != null -> {
            val call = functionCall ?: return null
            ToolCallPart(
                toolCallId = call.id ?: "gemini-tool-call-$index",
                functionName = call.name,
                argumentsJson = call.args.orEmpty().toJsonObject().properties
            )
        }
        functionResponse != null -> {
            val response = functionResponse ?: return null
            ToolResultPart(
                toolCallId = response.name,
                content = listOf(response.response.toJsonValue())
            )
        }
        else -> null
    }

private fun GeminiContent.toDomainMessage(): CommonRequestMessage =
    CommonRequestMessage(
        role = role.toCommonRole(),
        content = parts.mapIndexedNotNull { index, part -> part.toDomainPart(index) }
    )

private fun GeminiSystemInstruction.toSystemPrompt(): SystemPrompt? {
    val text = parts.mapNotNull { it.text?.takeIf(String::isNotBlank) }.joinToString("\n")
    return text.takeIf(String::isNotBlank)?.let(::SystemPrompt)
}

private fun GeminiFunctionDeclaration.toDomainTool(): CommonTool =
    CommonTool(
        name = name,
        description = description,
        parametersSchema = parameters.toJsonObject().properties
    )

private fun GeminiFunctionCallingConfig.toDomainToolChoice(): ToolChoice? {
    val allowed = allowedFunctionNames
    if (!allowed.isNullOrEmpty()) return ToolChoice.Specific(allowed)

    return when (mode.lowercase()) {
        "auto" -> ToolChoice.Auto
        "none" -> ToolChoice.None
        "any", "required" -> ToolChoice.Required
        else -> null
    }
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
                args = org.omniai.sdk.domain.common.json.JsonValue.JsonObject(part.argumentsJson).toRawMap(),
                id = part.toolCallId
            )
        )
        is JsonPart -> GeminiResponsePart(text = part.json.toString())
        is RefusalPart -> GeminiResponsePart(text = part.reason)
    }

private fun CommonUsage.toGeminiUsageMetadata(): GeminiUsageMetadata =
    GeminiUsageMetadata(
        promptTokenCount = inputTokens,
        candidatesTokenCount = outputTokens,
        totalTokenCount = totalTokens
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


