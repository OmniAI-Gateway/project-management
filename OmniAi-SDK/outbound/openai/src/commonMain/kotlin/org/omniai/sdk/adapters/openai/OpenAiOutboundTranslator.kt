package org.omniai.sdk.adapters.openai

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import org.omniai.sdk.contracts.openai.input.FunctionRef
import org.omniai.sdk.contracts.openai.input.OpenAiChatCompletionsRequest
import org.omniai.sdk.contracts.openai.input.OpenAiFunctionDefinition
import org.omniai.sdk.contracts.openai.input.OpenAiMessageInput
import org.omniai.sdk.contracts.openai.input.OpenAiResponseFormat
import org.omniai.sdk.contracts.openai.input.OpenAiStop
import org.omniai.sdk.contracts.openai.input.OpenAiTool
import org.omniai.sdk.contracts.openai.input.OpenAiToolCall
import org.omniai.sdk.contracts.openai.input.OpenAiToolCallFunction
import org.omniai.sdk.contracts.openai.input.OpenAiToolChoice
import org.omniai.sdk.contracts.openai.output.OpenAiChatCompletionsResponse
import org.omniai.sdk.contracts.openai.output.OpenAiChoice
import org.omniai.sdk.contracts.openai.output.OpenAiEventStream
import org.omniai.sdk.contracts.openai.output.OpenAiToolCallOutput
import org.omniai.sdk.contracts.openai.output.OpenAiUsage
import org.omniai.sdk.core.ports.OutboundTranslator
import org.omniai.sdk.domain.common.CommonRole
import org.omniai.sdk.domain.common.CommonTool
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.common.ToolChoice
import org.omniai.sdk.domain.common.content.JsonPart
import org.omniai.sdk.domain.common.content.ResponseContentPart
import org.omniai.sdk.domain.common.content.SharedContentPart
import org.omniai.sdk.domain.common.content.TextPart
import org.omniai.sdk.domain.common.content.ToolCallPart
import org.omniai.sdk.domain.common.content.ToolResultPart
import org.omniai.sdk.domain.common.json.JsonValue
import org.omniai.sdk.domain.common.json.toDomainJsonObject
import org.omniai.sdk.domain.common.json.toDomainJsonValue
import org.omniai.sdk.domain.common.json.toKotlinxJsonElement
import org.omniai.sdk.domain.common.json.toRawAny
import org.omniai.sdk.domain.common.json.toRawMap
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.requests.CommonRequestMessage
import org.omniai.sdk.domain.responses.ChoiceFinished
import org.omniai.sdk.domain.responses.ChoiceStarted
import org.omniai.sdk.domain.responses.CommonChoice
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent
import org.omniai.sdk.domain.responses.CommonResponseMessage
import org.omniai.sdk.domain.responses.CommonUsage
import org.omniai.sdk.domain.responses.FinishReason
import org.omniai.sdk.domain.responses.ResponseCompleted
import org.omniai.sdk.domain.responses.ResponseStarted
import org.omniai.sdk.domain.responses.TextDeltaEvent
import org.omniai.sdk.domain.responses.ToolCallArgumentsDeltaEvent
import org.omniai.sdk.domain.responses.ToolCallStartedEvent
import org.omniai.sdk.domain.responses.UsageReported
import org.omniai.sdk.domain.responses.ResponseErrored

class OpenAiOutboundTranslator : OutboundTranslator<OpenAiChatCompletionsRequest, OpenAiChatCompletionsResponse, OpenAiEventStream> {

    override fun fromDomain(domainRequest: CommonRequest): OpenAiChatCompletionsRequest {
        val providerOptions = domainRequest.providerOptions
        return OpenAiChatCompletionsRequest(
            model = domainRequest.model,
            messages = domainRequest.messages.map(CommonRequestMessage::toOpenAiMessageInput),
            temperature = domainRequest.config?.temperature,
            maxTokens = domainRequest.config?.maxTokens?.coerceAtMost(4000) ?: 1000,
            topP = domainRequest.config?.topP,
            stop = domainRequest.config?.stopSequences.toOpenAiStop(),
            frequencyPenalty = providerOptions.get<Double>("frequencyPenalty"),
            presencePenalty = providerOptions.get<Double>("presencePenalty"),
            n = providerOptions.get<Int>("n"),
            stream = providerOptions.get<Boolean>("stream"),
            seed = providerOptions.get<Int>("seed"),
            user = providerOptions.get<String>("user"),
            logitBias = providerOptions.get<Map<String, Int>>("logitBias"),
            logProbs = providerOptions.get<Boolean>("logProbs"),
            topLogProbs = providerOptions.get<Int>("topLogProbs"),
            responseFormat = domainRequest.takeIf { it.jsonResponse }?.let { OpenAiResponseFormat(type = "json_object") },
            tools = domainRequest.tools.map(CommonTool::toOpenAiTool).ifEmpty { null },
            toolChoice = domainRequest.toolChoice?.toOpenAiToolChoice()
        )
    }

    override fun toDomain(providerResponse: OpenAiChatCompletionsResponse): CommonResponse =
        CommonResponse(
            provider = Provider.OPENAI,
            id = providerResponse.id,
            model = providerResponse.model,
            choices = providerResponse.choices.map(::toDomainChoice),
            usage = providerResponse.usage?.toDomainUsage(),
            providerOptions = buildMap {
                put("created", providerResponse.created)
                providerResponse.systemFingerprint?.let { put("systemFingerprint", it) }
            }
        )

    override fun toDomainEvent(providerEvent: OpenAiEventStream): CommonResponseEvent =
        when (providerEvent) {
            is OpenAiEventStream.Chunk -> providerEvent.data.toDomainChunkEvent()
            OpenAiEventStream.Done -> ResponseCompleted(
                provider = Provider.OPENAI,
                id = null,
                model = Model(""),
                sequence = 0L,
                providerEventType = "done"
            )
            is OpenAiEventStream.Error -> ResponseErrored(
                provider = Provider.OPENAI,
                id = null,
                model = Model(""),
                sequence = 0L,
                message = providerEvent.error.message,
                retryable = providerEvent.error.type.isRetryableOpenAiError(),
                providerEventType = "error"
            )
        }

}

private fun List<String>?.toOpenAiStop(): OpenAiStop? = this?.takeIf { it.isNotEmpty() }?.let { stops ->
        if (stops.size == 1) OpenAiStop.Single(stops.first()) else OpenAiStop.Multiple(stops)
    }

/*
For now just supporting text we use string but to support text+image or sound just need to use array
 */

private fun CommonRequestMessage.toOpenAiMessageInput(): OpenAiMessageInput {
    val textContent = content.filterIsInstance<TextPart>().joinToString("\n") { it.text }.ifBlank { null }
    val toolCalls = content.mapNotNull { it as? ToolCallPart }.map {
        OpenAiToolCall(
            id = it.toolCallId,
            function = OpenAiToolCallFunction(
                name = it.functionName,
                arguments = JsonValue.JsonObject(it.argumentsJson).toRawMap().toString()
            )
        )
    }.ifEmpty { null }
    val toolResult = content.firstOrNull { it is ToolResultPart } as? ToolResultPart
    val toolResultContent = toolResult?.content?.firstOrNull()?.toRawAny()?.toString()
    return OpenAiMessageInput(
        role = role.toOpenAiRole(),
        content = if (role == CommonRole.TOOL) toolResultContent else textContent,
        toolCalls = toolCalls,
        toolCallId = if (role == CommonRole.TOOL) toolResult?.toolCallId else null
    )
}

private fun CommonTool.toOpenAiTool(): OpenAiTool =
    OpenAiTool(
        function = OpenAiFunctionDefinition(
            name = name,
            description = description,
            parameters = parametersSchema.toOpenAiJsonObject()
        )
    )

private fun ToolChoice.toOpenAiToolChoice(): OpenAiToolChoice =
    when (this) {
        ToolChoice.Auto -> OpenAiToolChoice.Mode("auto")
        ToolChoice.None -> OpenAiToolChoice.Mode("none")
        ToolChoice.Required -> OpenAiToolChoice.Mode("required")
        is ToolChoice.Specific -> OpenAiToolChoice.Function(function = FunctionRef(toolNames.first()))
    }

private fun toDomainChoice(choice: OpenAiChoice): CommonChoice {
    val role = (choice.message?.role ?: choice.delta?.role ?: "assistant").toCommonRole()
    val parts = mutableListOf<ResponseContentPart>()
    choice.message?.content?.takeIf(String::isNotBlank)?.let { parts += it.toDomainContentPart() }
    choice.delta?.content?.takeIf(String::isNotBlank)?.let { parts += it.toDomainContentPart()}
    choice.message?.toolCalls.orEmpty().mapTo(parts) { it.toDomainToolCallPart() }
    choice.delta?.toolCalls.orEmpty().mapTo(parts) { it.toDomainToolCallPart() }

    return CommonChoice(
        index = choice.index,
        message = CommonResponseMessage(
            role = role,
            content = parts
        ),
        finishReason = choice.finishReason.toDomainFinishReason()
    )
}

private fun String.toDomainContentPart(): SharedContentPart{
    val trimmed = trim()

    if(trimmed.startsWith("[") && trimmed.endsWith("]") ||
        trimmed.startsWith("{") && trimmed.endsWith("}")){
        try {
            Json.parseToJsonElement(trimmed).let {
                jsonElement -> return JsonPart(jsonElement.toDomainJsonValue())
            }
        }catch ( _ : Exception){
            return TextPart(this)
        }
    }
    return TextPart(this)
}

private fun OpenAiToolCallOutput.toDomainToolCallPart(): ToolCallPart =
    ToolCallPart(
        toolCallId = id,
        functionName = function.name ?: "unknown",
        argumentsJson = function.arguments.toDomainToolArguments()
    )


private fun OpenAiUsage.toDomainUsage(): CommonUsage =
    CommonUsage(
        inputTokens = promptTokens,
        outputTokens = completionTokens,
        totalTokens = totalTokens
    )


private fun String.toCommonRole(): CommonRole =
    when (lowercase()) {
        "system" -> CommonRole.SYSTEM
        "assistant" -> CommonRole.ASSISTANT
        "tool" -> CommonRole.TOOL
        "user" -> CommonRole.USER
        else -> CommonRole.USER
    }

private fun String?.toDomainFinishReason(): FinishReason? =
    when (this?.lowercase()) {
        "stop" -> FinishReason.STOP
        "length" -> FinishReason.LENGTH
        "tool_calls" -> FinishReason.TOOL_CALL
        "content_filter" -> FinishReason.CONTENT_FILTER
        null -> null
        else -> FinishReason.OTHER
    }

private fun CommonRole.toOpenAiRole(): String =
    when (this) {
        CommonRole.SYSTEM -> "system"
        CommonRole.USER -> "user"
        CommonRole.ASSISTANT -> "assistant"
        CommonRole.TOOL -> "tool"
    }

private fun Map<String, JsonValue>.toOpenAiJsonObject(): JsonObject =
    JsonObject(entries.associate { (key, value) -> key to value.toKotlinxJsonElement() })

private fun String.toDomainToolArguments(): Map<String, JsonValue> {
    val parsed = runCatching { Json.parseToJsonElement(this) }.getOrNull()
    val jsonObject = parsed as? JsonObject
    return jsonObject?.toDomainJsonObject()?.properties ?: mapOf("raw" to JsonValue.JsonString(this))
}


private fun OpenAiChatCompletionsResponse.toDomainChunkEvent(): CommonResponseEvent {
    val model = Model(model)
    val sequence = created
    val firstChoice = choices.firstOrNull()

    if (obj != "chat.completion.chunk") {
        return ResponseCompleted(
            provider = Provider.OPENAI,
            id = id,
            model = model,
            sequence = sequence,
            providerEventType = obj
        )
    }

    if (choices.isEmpty()) {
        val usage = usage
        if (usage != null) {
            return UsageReported(
                provider = Provider.OPENAI,
                id = id,
                model = model,
                sequence = sequence,
                usage = usage.toDomainUsage(),
                providerEventType = obj
            )
        }

        return ResponseStarted(
            provider = Provider.OPENAI,
            id = id,
            model = model,
            sequence = sequence,
            providerEventType = obj
        )
    }

    val choice = firstChoice ?: return ResponseCompleted(
        provider = Provider.OPENAI,
        id = id,
        model = model,
        sequence = sequence,
        providerEventType = obj
    )

    choice.finishReason?.let {
        return ChoiceFinished(
            provider = Provider.OPENAI,
            id = id,
            model = model,
            sequence = sequence,
            choiceIndex = choice.index,
            finishReason = it.toDomainFinishReason(),
            providerEventType = obj
        )
    }

    val toolCall = choice.delta?.toolCalls?.firstOrNull()
    if (toolCall != null) {
        val partialJson = toolCall.function.arguments
        val functionName = toolCall.function.name
        if (partialJson.isNotBlank() && functionName.isNullOrBlank()) {
            return ToolCallArgumentsDeltaEvent(
                provider = Provider.OPENAI,
                id = id,
                model = model,
                sequence = sequence,
                choiceIndex = choice.index,
                toolCallIndex = toolCall.index ?: 0,
                argumentsFragment = partialJson,
                providerEventType = obj
            )
        }

        return ToolCallStartedEvent(
            provider = Provider.OPENAI,
            id = id,
            model = model,
            sequence = sequence,
            choiceIndex = choice.index,
            toolCallIndex = toolCall.index ?: 0,
            toolCallId = toolCall.id,
            functionName = functionName ?: "unknown",
            providerEventType = obj
        )
    }

    choice.delta?.content?.let {
        return TextDeltaEvent(
            provider = Provider.OPENAI,
            id = id,
            model = model,
            sequence = sequence,
            choiceIndex = choice.index,
            text = it,
            providerEventType = obj
        )
    }

    choice.delta?.role?.let {
        return ChoiceStarted(
            provider = Provider.OPENAI,
            id = id,
            model = model,
            sequence = sequence,
            choiceIndex = choice.index,
            role = it.toCommonRole(),
            providerEventType = obj
        )
    }

    return ResponseCompleted(
        provider = Provider.OPENAI,
        id = id,
        model = model,
        sequence = sequence,
        providerEventType = obj
    )
}

private fun String?.isRetryableOpenAiError(): Boolean =
    this?.lowercase() in setOf("server_error", "rate_limit_error", "temporarily_unavailable")
