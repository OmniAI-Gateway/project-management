package org.omniai.sdk.adapters.gemini

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.runningFold
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
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
import org.omniai.sdk.contracts.gemini.output.GeminiCandidate
import org.omniai.sdk.contracts.gemini.output.GeminiEventStream
import org.omniai.sdk.contracts.gemini.output.GeminiGenerateContentResponse
import org.omniai.sdk.contracts.gemini.output.GeminiResponsePart
import org.omniai.sdk.contracts.gemini.output.GeminiUsageMetadata
import org.omniai.sdk.core.ports.OutboundTranslator
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
import org.omniai.sdk.domain.common.json.toDomainJsonObject
import org.omniai.sdk.domain.common.json.toKotlinxJsonElement
import org.omniai.sdk.domain.common.json.toKotlinxJsonObject
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
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class GeminiOutboundTranslator : OutboundTranslator<GeminiGenerateContentRequest, GeminiGenerateContentResponse, GeminiEventStream> {

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
                topK = providerOptions.get<Int>("topK"),
                thinkingConfig = providerOptions.get<GeminiThinkingConfig>("thinkingConfig"),
                responseMimeType = when {
                    domainRequest.jsonResponse -> "application/json"
                    else -> providerOptions.get<String>("responseMimeType")
                },
                responseJsonSchema = providerOptions.get<JsonObject>("responseJsonSchema")
            )
        )
    }

    override fun toDomain(providerResponse: GeminiGenerateContentResponse): CommonResponse =
        CommonResponse(
            provider = Provider.GEMINI,
            id = providerResponse.responseId ?: generateGeminiId(),
            model = providerResponse.modelVersion ?: "",
            choices = providerResponse.candidates.map(::toDomainChoice),
            usage = providerResponse.usageMetadata?.toDomainUsage(),
            providerOptions = providerResponse.promptFeedback?.let { mapOf("promptFeedback" to it.blockReason) } ?: emptyMap()
        )

    override fun toDomainEvent(providerEvent: Flow<GeminiEventStream>): Flow<CommonResponseEvent> =
        providerEvent
            .runningFold(GeminiEventContext()) { context, event ->
                val translatedEvent = event.toDomainStreamEvent(context.id, context.model)
                context.copy(id = translatedEvent.id, model = translatedEvent.model, event = translatedEvent)
            }
            .mapNotNull { it.event }


}

@OptIn(ExperimentalUuidApi::class)
private fun generateGeminiId() = Uuid.random().toHexString()

private data class GeminiEventContext(
    val id: String = "",
    val model: Model = Model(""),
    val event: CommonResponseEvent? = null
)

private fun GeminiEventStream.toDomainStreamEvent(previousId: String, previousModel: Model): CommonResponseEvent =
    when (this) {
        is GeminiEventStream.Chunk -> data.toDomainChunkEvent(previousId, previousModel)
        GeminiEventStream.Done -> ResponseCompleted(
            provider = Provider.GEMINI,
            id = previousId,
            model = previousModel,
            sequence = 0L,
            providerEventType = "done"
        )
        is GeminiEventStream.Error -> {
            val streamError = error.error
            ResponseErrored(
                provider = Provider.GEMINI,
                id = previousId,
                model = previousModel,
                sequence = 0L,
                message = streamError.message,
                retryable = streamError.status.isRetryableGeminiStatus(),
                providerEventType = "error"
            )
        }
    }

private fun GeminiGenerateContentResponse.toDomainChunkEvent(previousId: String, previousModel: Model): CommonResponseEvent {
    val resolvedId = responseId?.takeUnless { it.isBlank() } ?: previousId
    val model = modelVersion?.takeUnless { it.isBlank() }?.let(::Model) ?: previousModel
    val sequence = 0L
    val firstCandidate = candidates.firstOrNull()

    promptFeedback?.blockReason?.let { blockReason ->
        return ResponseErrored(
            provider = Provider.GEMINI,
                id = resolvedId,
            model = model,
            sequence = sequence,
            message = blockReason,
            retryable = blockReason.contains("TRANSIENT", ignoreCase = true),
            providerEventType = "prompt_feedback"
        )
    }

    if (candidates.isEmpty()) {
        usageMetadata?.let {
            return UsageReported(
                provider = Provider.GEMINI,
                id = resolvedId,
                model = model,
                sequence = sequence,
                usage = it.toDomainUsage(),
                providerEventType = "usage"
            )
        }

        return ResponseStarted(
            provider = Provider.GEMINI,
            id = resolvedId,
            model = model,
            sequence = sequence,
            providerEventType = "response_start"
        )
    }

    val candidate = firstCandidate ?: return ResponseStarted(
        provider = Provider.GEMINI,
        id = resolvedId,
        model = model,
        sequence = sequence,
        providerEventType = "response_start"
    )

    candidate.finishReason?.let {
        return ChoiceFinished(
            provider = Provider.GEMINI,
            id = resolvedId,
            model = model,
            sequence = sequence,
            choiceIndex = candidate.index ?: 0,
            finishReason = it.toDomainFinishReason(),
            providerEventType = "choice_finished"
        )
    }

    val functionCall = candidate.content?.parts.orEmpty().firstNotNullOfOrNull { it.functionCall }
    if (functionCall != null) {
        val argumentsFragment = functionCall.args.extractArgumentsFragment()
        if (argumentsFragment != null && functionCall.name.isBlank()) {
            return ToolCallArgumentsDeltaEvent(
                provider = Provider.GEMINI,
                id = resolvedId,
                model = model,
                sequence = sequence,
                choiceIndex = candidate.index ?: 0,
                toolCallIndex = 0,
                argumentsFragment = argumentsFragment,
                providerEventType = "tool_call_arguments_delta"
            )
        }

        return ToolCallStartedEvent(
            provider = Provider.GEMINI,
            id = resolvedId,
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
            id = resolvedId,
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
            id = resolvedId,
            model = model,
            sequence = sequence,
            choiceIndex = candidate.index ?: 0,
            role = it.toCommonRole(),
            providerEventType = "choice_started"
        )
    }

    return ResponseStarted(
        provider = Provider.GEMINI,
        id = resolvedId,
        model = model,
        sequence = sequence,
        providerEventType = "response_start"
    )
}

private fun JsonObject?.extractArgumentsFragment(): String? {
    val args = this ?: return null
    val partialJson = (args["partialJson"] as? JsonPrimitive)?.contentOrNull
    if (partialJson != null) return partialJson
    if (args.isEmpty()) return null
    return args.toString()
}

private fun String?.isRetryableGeminiStatus(): Boolean {
    val status = this?.uppercase() ?: return false
    return status in setOf("UNAVAILABLE", "RESOURCE_EXHAUSTED", "DEADLINE_EXCEEDED", "ABORTED", "INTERNAL")
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
            thoughtSignature =  "skip_thought_signature_validator",
            functionCall = GeminiInputFunctionCall(
                id = toolCallId,
                name = functionName,
                args = JsonValue.JsonObject(argumentsJson).toKotlinxJsonObject(),
                thoughtSignature = "skip_thought_signature_validator"
            )
        )
        is ToolResultPart -> GeminiPart(
            functionResponse = GeminiFunctionResponse(
                name = toolCallId,
                response = content.firstOrNull().toGeminiToolResponseJson()
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
                    parameters = JsonValue.JsonObject(it.parametersSchema).toKotlinxJsonObject().cleanGeminiParameters()
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
        finishReason = if (hasToolCalls) {
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
                argumentsJson = fc.args?.toDomainJsonObject()?.properties.orEmpty()
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
private fun JsonObject.cleanGeminiParameters(): JsonObject {
    val keysToRemove = setOf(
        "\$schema",
        "additionalProperties",
        "propertyNames",
        "title",
        "default",
        "\$id",
        "exclusiveMinimum"
    )

    val cleaned = mutableMapOf<String, JsonElement>()

    for ((key, value) in this) {
        if (key in keysToRemove) continue
        if (key == "const") {
            cleaned["enum"] = JsonArray(listOf(value))
            continue
        }
        val cleanedValue = value.cleanGeminiElement(keysToRemove)
        cleaned[key] = cleanedValue
    }

    return JsonObject(cleaned)
}

private fun JsonElement.cleanGeminiElement(keysToRemove: Set<String>): JsonElement =
    when (this) {
        is JsonObject -> JsonObject(
            entries
                .filterNot { (key, _) -> key in keysToRemove || key == "const" }
                .associate { (key, value) -> key to value.cleanGeminiElement(keysToRemove) }
                .toMutableMap()
                .apply {
                    this@cleanGeminiElement["const"]?.let { constValue ->
                        this["enum"] = JsonArray(listOf(constValue.cleanGeminiElement(keysToRemove)))
                    }
                }
        )

        is JsonArray -> JsonArray(map { it.cleanGeminiElement(keysToRemove) })
        else -> this
    }

private fun JsonValue?.toGeminiToolResponseJson(): JsonObject {
    val value = this ?: return JsonObject(mapOf("value" to JsonNull))
    return when (value) {
        is JsonValue.JsonObject -> value.toKotlinxJsonObject()
        else -> JsonObject(mapOf("value" to value.toKotlinxJsonElement()))
    }
}

