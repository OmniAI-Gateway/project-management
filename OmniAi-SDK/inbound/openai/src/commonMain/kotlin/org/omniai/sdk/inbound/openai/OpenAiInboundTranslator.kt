package org.omniai.sdk.inbound.openai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
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
import org.omniai.sdk.common.TypedMap
import org.omniai.sdk.ports.inbound.InboundTranslator
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
import org.omniai.sdk.domain.common.json.toDomainJsonObject
import org.omniai.sdk.domain.common.json.toKotlinxJsonElement
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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class OpenAiInboundTranslator :
    InboundTranslator<OpenAiChatCompletionsRequest, OpenAiChatCompletionsResponse, OpenAiChatCompletionsResponse> {

    override val provider: Provider = Provider.OPENAI

    override fun toDomain(clientRequest: OpenAiChatCompletionsRequest): CommonRequest {
        val responseFormatType = clientRequest.responseFormat?.type?.lowercase()
        val config = CommonGenerationConfig(
            temperature = clientRequest.temperature,
            maxTokens = clientRequest.maxTokens,
            topP = clientRequest.topP,
            stopSequences = clientRequest.stop.toStopSequences()
        )
        return CommonRequest(
            provider = provider,
            model = clientRequest.model,
            messages = clientRequest.messages.map(OpenAiMessageInput::toDomainMessage),
            config = config,
            tools = clientRequest.tools?.map(OpenAiTool::toDomainTool).orEmpty(),
            toolChoice = clientRequest.toolChoice?.toDomainToolChoice(),
            jsonResponse = responseFormatType == "json_object" || responseFormatType == "json_schema",
            providerOptions = clientRequest.buildToDomainTypeMap()
        )
    }


    override fun fromDomain(domainResponse: CommonResponse): OpenAiChatCompletionsResponse =
        OpenAiChatCompletionsResponse(
            id = domainResponse.id?.takeIf { it.isNotBlank() } ?: generateOpenAiID(),
            obj = "chat.completion",
            created = ((domainResponse.providerOptions["created"] as? Number)?.toLong() ?: getTime()),
            model = domainResponse.model,
            systemFingerprint = domainResponse.providerOptions["systemFingerprint"] as? String,
            choices = domainResponse.choices.map(::toOpenAiChoice),
            usage = domainResponse.usage?.toOpenAiUsage()
        )

    override fun fromDomainEvent(domainEvent: Flow<CommonResponseEvent>): Flow<OpenAiChatCompletionsResponse> =
        domainEvent.map(::toOpenAiEvent)
}

private fun toOpenAiEvent(domainEvent: CommonResponseEvent): OpenAiChatCompletionsResponse =
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
                                    index = domainEvent.toolCallIndex,
                                    type = "function",
                                    function = OpenAiToolCallFunctionOutput(
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

            // this is not ritgh
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

@OptIn(ExperimentalUuidApi::class)
private fun generateOpenAiID(): String {
    return "chatcmpl-${Uuid.random().toHexString()}"
}

@OptIn(ExperimentalTime::class)
private fun getTime() = Clock.System.now().epochSeconds

private fun OpenAiChatCompletionsRequest.buildToDomainTypeMap(): TypedMap = TypedMap().apply {
    stream?.let { put("stream", it) }
    frequencyPenalty?.let { put("frequencyPenalty", it) }
    presencePenalty?.let { put("presencePenalty", it) }
    n?.let { put("n", it) }
    seed?.let { put("seed", it) }
    user?.let { put("user", it) }
    logitBias?.let { put("logitBias", it) }
    logProbs?.let { put("logProbs", it) }
    topLogProbs?.let { put("topLogProbs", it) }
    responseFormat?.let { put("responseFormat", it) }
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
    id: String,
    model: String,
    choices: List<OpenAiChoice>,
    usage: OpenAiUsage? = null
): OpenAiChatCompletionsResponse =
    OpenAiChatCompletionsResponse(
        id = id,
        obj = "chat.completion.chunk",
        created = getTime(),
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

private fun Map<String, JsonValue>.toOpenAiJsonObject(): JsonObject =
    JsonObject(entries.associate { (key, value) -> key to value.toKotlinxJsonElement() })
