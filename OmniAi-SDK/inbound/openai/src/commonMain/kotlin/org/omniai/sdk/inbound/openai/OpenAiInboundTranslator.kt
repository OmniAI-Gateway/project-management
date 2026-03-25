package org.omniai.sdk.inbound.openai
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.omniai.sdk.contracts.openai.input.OpenAiChatCompletionsRequest
import org.omniai.sdk.contracts.openai.input.OpenAiMessageInput
import org.omniai.sdk.contracts.openai.input.OpenAiStop
import org.omniai.sdk.contracts.openai.input.OpenAiTool
import org.omniai.sdk.contracts.openai.input.OpenAiToolCall
import org.omniai.sdk.contracts.openai.input.OpenAiToolChoice
import org.omniai.sdk.contracts.openai.output.OpenAiChatCompletionsResponse
import org.omniai.sdk.contracts.openai.output.OpenAiChoice
import org.omniai.sdk.contracts.openai.output.OpenAiDelta
import org.omniai.sdk.contracts.openai.output.OpenAiMessageOutput
import org.omniai.sdk.contracts.openai.output.OpenAiToolCallFunctionOutput
import org.omniai.sdk.contracts.openai.output.OpenAiToolCallOutput
import org.omniai.sdk.contracts.openai.output.OpenAiUsage
import org.omniai.sdk.core.ports.InboundTranslator
import org.omniai.sdk.domain.common.CommonGenerationConfig
import org.omniai.sdk.domain.common.CommonRole
import org.omniai.sdk.domain.common.CommonTool
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.common.ToolChoice
import org.omniai.sdk.domain.common.content.JsonPart
import org.omniai.sdk.domain.common.content.RefusalPart
import org.omniai.sdk.domain.common.content.ResponseContentPart
import org.omniai.sdk.domain.common.content.TextPart
import org.omniai.sdk.domain.common.content.ToolCallPart
import org.omniai.sdk.domain.common.content.ToolResultPart
import org.omniai.sdk.domain.common.json.JsonValue
import org.omniai.sdk.domain.common.json.toRawAny
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


class OpenAiInboundTranslator :
    InboundTranslator<OpenAiChatCompletionsRequest, OpenAiChatCompletionsResponse, OpenAiChatCompletionsResponse> {
    override val provider: Provider = Provider.OPENAI

    override fun toDomain(clientRequest: OpenAiChatCompletionsRequest): CommonRequest {
        val providerOptions = buildMap {
            if (clientRequest.stream != null) put("stream", clientRequest.stream)
            if (clientRequest.frequencyPenalty != null) put("frequencyPenalty", clientRequest.frequencyPenalty)
            if (clientRequest.presencePenalty != null) put("presencePenalty", clientRequest.presencePenalty)
            if (clientRequest.n != null) put("n", clientRequest.n)
            if (clientRequest.seed != null) put("seed", clientRequest.seed)
            if (clientRequest.user != null) put("user", clientRequest.user)
            if (clientRequest.logitBias != null) put("logitBias", clientRequest.logitBias)
            if (clientRequest.logProbs != null) put("logProbs", clientRequest.logProbs)
            if (clientRequest.topLogProbs != null) put("topLogProbs", clientRequest.topLogProbs)
            if (clientRequest.responseFormat != null) put("responseFormat", clientRequest.responseFormat)
        }

        val responseFormatType = clientRequest.responseFormat?.type?.lowercase()
        return CommonRequest(
            provider = provider,
            model = clientRequest.model,
            messages = clientRequest.messages.map(OpenAiMessageInput::toDomainMessage),
            config = CommonGenerationConfig(
                temperature = clientRequest.temperature,
                maxTokens = clientRequest.maxTokens,
                topP = clientRequest.topP,
                stopSequences = clientRequest.stop.toStopSequences()
            ),
            tools = clientRequest.tools?.map(OpenAiTool::toDomainTool).orEmpty(),
            toolChoice = clientRequest.toolChoice?.toDomainToolChoice(),
            jsonResponse = responseFormatType == "json_object" || responseFormatType == "json_schema",
            providerOptions = providerOptions
        )
    }

    override fun fromDomain(domainResponse: CommonResponse): OpenAiChatCompletionsResponse =
        OpenAiChatCompletionsResponse(
            id = domainResponse.id?.takeIf { it.isNotBlank() } ?: "chatcmpl_${currentTimeMillis()}",
            obj = "chat.completion",
            created = ((domainResponse.providerOptions["created"] as? Number)?.toLong() ?: (currentTimeMillis() / 1000)),
            model = domainResponse.model,
            systemFingerprint = domainResponse.providerOptions["systemFingerprint"] as? String,
            choices = domainResponse.choices.map(::toOpenAiChoice),
            usage = domainResponse.usage?.toOpenAiUsage()
        )

    override fun fromDomainEvent(domainEvent: CommonResponseEvent): OpenAiChatCompletionsResponse =
        when (domainEvent) {
            is ResponseStarted -> chunkResponse(
                id = domainEvent.id,
                model = domainEvent.model.model,
                choices = emptyList()
            )
            is ChoiceStarted -> chunkResponse(
                id = domainEvent.id,
                model = domainEvent.model.model,
                choices = listOf(
                    OpenAiChoice(
                        index = domainEvent.choiceIndex,
                        delta = OpenAiDelta(role = domainEvent.role?.toOpenAiRole() ?: "assistant")
                    )
                )
            )
            is TextDeltaEvent -> chunkResponse(
                id = domainEvent.id,
                model = domainEvent.model.model,
                choices = listOf(
                    OpenAiChoice(
                        index = domainEvent.choiceIndex,
                        delta = OpenAiDelta(content = domainEvent.text)
                    )
                )
            )
            is ToolCallStartedEvent -> chunkResponse(
                id = domainEvent.id,
                model = domainEvent.model.model,
                choices = listOf(
                    OpenAiChoice(
                        index = domainEvent.choiceIndex,
                        delta = OpenAiDelta(
                            toolCalls = listOf(
                                OpenAiToolCallOutput(
                                    id = domainEvent.toolCallId,
                                    index = domainEvent.toolCallIndex,
                                    type = "function",
                                    function = OpenAiToolCallFunctionOutput(
                                        name = domainEvent.functionName,
                                        arguments = ""
                                    )
                                )
                            )
                        )
                    )
                )
            )
            is ToolCallArgumentsDeltaEvent -> chunkResponse(
                id = domainEvent.id,
                model = domainEvent.model.model,
                choices = listOf(
                    OpenAiChoice(
                        index = domainEvent.choiceIndex,
                        delta = OpenAiDelta(
                            toolCalls = listOf(
                                OpenAiToolCallOutput(
                                    id = "openai-tool-call-${domainEvent.toolCallIndex}",
                                    index = domainEvent.toolCallIndex,
                                    type = "function",
                                    function = OpenAiToolCallFunctionOutput(
                                        name = "",
                                        arguments = domainEvent.argumentsFragment
                                    )
                                )
                            )
                        )
                    )
                )
            )
            is ChoiceFinished -> chunkResponse(
                id = domainEvent.id,
                model = domainEvent.model.model,
                choices = listOf(
                    OpenAiChoice(
                        index = domainEvent.choiceIndex,
                        finishReason = domainEvent.finishReason.toOpenAiFinishReason()
                    )
                )
            )
            is UsageReported -> chunkResponse(
                id = domainEvent.id,
                model = domainEvent.model.model,
                choices = emptyList(),
                usage = domainEvent.usage.toOpenAiUsage()
            )
            is ResponseCompleted -> chunkResponse(
                id = domainEvent.id,
                model = domainEvent.model.model,
                choices = emptyList()
            )
            is ResponseErrored -> chunkResponse(
                id = domainEvent.id,
                model = domainEvent.model.model,
                choices = listOf(
                    OpenAiChoice(
                        index = 0,
                        delta = OpenAiDelta(content = domainEvent.message)
                    )
                )
            )
        }
}

private fun OpenAiStop?.toStopSequences(): List<String>? =
    when (this) {
        is OpenAiStop.Single -> listOf(value)
        is OpenAiStop.Multiple -> values
        null -> null
    }

private fun String.toCommonRole(): CommonRole =
    when (lowercase()) {
        "system" -> CommonRole.SYSTEM
        "assistant" -> CommonRole.ASSISTANT
        "tool" -> CommonRole.TOOL
        "user" -> CommonRole.USER
        else -> CommonRole.USER
    }

private fun OpenAiToolCall.toDomainPart(index: Int): ToolCallPart =
    ToolCallPart(
        toolCallId = id ?: "openai-tool-call-$index",
        functionName = function.name,
        argumentsJson = mapOf("raw" to JsonValue.JsonString(function.arguments))
    )

private fun OpenAiMessageInput.toDomainMessage(): CommonRequestMessage {
    val commonRole = role.toCommonRole()
    return CommonRequestMessage(
        role = commonRole,
        content = buildList {
            val rawContent = content
            val rawToolCallId = toolCallId
            content?.takeIf { it.isNotBlank() }?.let { add(TextPart(it)) }
            toolCalls.orEmpty().forEachIndexed { index, toolCall -> add(toolCall.toDomainPart(index)) }
            if (commonRole == CommonRole.TOOL && rawToolCallId != null && rawContent != null) {
                add(ToolResultPart(toolCallId = rawToolCallId, content = listOf(rawContent.toJsonValue())))
            }
        }
    )
}

private fun OpenAiTool.toDomainTool(): CommonTool =
    CommonTool(
        name = function.name,
        description = function.description ?: function.name,
        parametersSchema = function.parameters?.toDomainJsonObject()?.properties.orEmpty()
    )

private fun OpenAiToolChoice.toDomainToolChoice(): ToolChoice? =
    when (this) {
        is OpenAiToolChoice.Mode -> value.toToolChoice()
        is OpenAiToolChoice.Function -> ToolChoice.Specific(function.name)
    }

private fun String.toToolChoice(): ToolChoice? =
    when (lowercase()) {
        "auto" -> ToolChoice.Auto
        "none" -> ToolChoice.None
        "required" -> ToolChoice.Required
        else -> null
    }

private fun chunkResponse(
    id: String?,
    model: String,
    choices: List<OpenAiChoice>,
    usage: OpenAiUsage? = null
): OpenAiChatCompletionsResponse =
    OpenAiChatCompletionsResponse(
        id = id ?: "catchall_${currentTimeMillis()}",
        obj = "chat.completion.chunk",
        created = currentTimeMillis() / 1000,
        model = model,
        choices = choices,
        usage = usage
    )

private fun toOpenAiChoice(choice: CommonChoice): OpenAiChoice {
    val toolCalls = choice.message.content.mapNotNull(::toOpenAiToolCall).ifEmpty { null }
    val content = choice.message.content.firstRenderableContentOrNull()
    return OpenAiChoice(
        index = choice.index,
        message = OpenAiMessageOutput(
            role = choice.message.role.toOpenAiRole(),
            content = content,
            toolCalls = toolCalls
        ),
        finishReason = choice.finishReason.toOpenAiFinishReason()
    )
}

private fun List<ResponseContentPart>.firstRenderableContentOrNull(): String? =
    filterIsInstance<TextPart>()
        .joinToString("\n") { it.text }
        .trim()
        .takeIf { it.isNotEmpty() }
        ?: filterIsInstance<RefusalPart>().firstOrNull()?.reason
        ?: filterIsInstance<JsonPart>().firstOrNull()?.json?.toRawAny()?.toString()


private fun toOpenAiToolCall(part: ResponseContentPart): OpenAiToolCallOutput? =
    when (part) {
        is ToolCallPart -> OpenAiToolCallOutput(
            id = part.toolCallId,
            type = "function",
            function = OpenAiToolCallFunctionOutput(
                name = part.functionName,
                arguments = part.argumentsJson.toOpenAiJsonObject().toString()
            )
        )
        is TextPart, is JsonPart, is RefusalPart -> null
    }

private fun CommonUsage.toOpenAiUsage(): OpenAiUsage =
    OpenAiUsage(
        totalTokens = totalTokens,
        completionTokens = outputTokens,
        promptTokens = inputTokens
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

// temos no inbound e outbound, secalhar criar um helper faria sentido.
private fun Map<String, JsonValue>.toOpenAiJsonObject(): JsonObject =
    JsonObject(entries.associate { (key, value) -> key to value.toOpenAiJsonElement() })

private fun JsonValue.toOpenAiJsonElement(): JsonElement =
    when (this) {
        is JsonValue.JsonObject -> JsonObject(properties.mapValues { (_, value) -> value.toOpenAiJsonElement() })
        is JsonValue.JsonArray -> JsonArray(items.map(JsonValue::toOpenAiJsonElement))
        is JsonValue.JsonString -> JsonPrimitive(value)
        is JsonValue.JsonNumber -> JsonPrimitive(value)
        is JsonValue.JsonBoolean -> JsonPrimitive(value)
        JsonValue.JsonNull -> JsonNull
    }

private fun JsonObject.toDomainJsonObject(): JsonValue.JsonObject =
    JsonValue.JsonObject(properties = mapValues { (_, value) -> value.toDomainJsonValue() })

private fun JsonElement.toDomainJsonValue(): JsonValue =
    when (this) {
        is JsonObject -> JsonValue.JsonObject(properties = mapValues { (_, value) -> value.toDomainJsonValue() })
        is JsonArray -> JsonValue.JsonArray(items = map(JsonElement::toDomainJsonValue))
        is JsonPrimitive -> when {
            isString -> JsonValue.JsonString(content)
            content == "true" -> JsonValue.JsonBoolean(true)
            content == "false" -> JsonValue.JsonBoolean(false)
            content.toLongOrNull() != null -> JsonValue.JsonNumber(content.toLong())
            content.toDoubleOrNull() != null -> JsonValue.JsonNumber(content.toDouble())
            else -> JsonValue.JsonString(content)
        }
        JsonNull -> JsonValue.JsonNull
    }
