package org.omniai.sdk.adapters.anthropic

import org.omniai.sdk.contracts.anthropic.input.AnthropicContent
import org.omniai.sdk.contracts.anthropic.input.AnthropicInputContentBlock
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessageInput
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.input.AnthropicThinkingConfig
import org.omniai.sdk.contracts.anthropic.input.AnthropicToolChoice
import org.omniai.sdk.contracts.anthropic.input.AnthropicToolDefinition
import org.omniai.sdk.contracts.anthropic.input.ListContentBlock
import org.omniai.sdk.contracts.anthropic.input.RawText
import org.omniai.sdk.contracts.anthropic.output.AnthropicMessageResponse
import org.omniai.sdk.contracts.anthropic.output.AnthropicOutputContent
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamDelta
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent
import org.omniai.sdk.contracts.anthropic.output.AnthropicUsage
import org.omniai.sdk.core.ports.OutboundTranslator
import org.omniai.sdk.domain.common.CommonRole
import org.omniai.sdk.domain.common.CommonTool
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.common.ToolChoice
import org.omniai.sdk.domain.common.content.JsonPart
import org.omniai.sdk.domain.common.content.RequestContentPart
import org.omniai.sdk.domain.common.content.RefusalPart
import org.omniai.sdk.domain.common.content.ResponseContentPart
import org.omniai.sdk.domain.common.content.TextPart
import org.omniai.sdk.domain.common.content.ToolCallPart
import org.omniai.sdk.domain.common.content.ToolResultPart
import org.omniai.sdk.domain.common.json.JsonValue
import org.omniai.sdk.domain.common.json.toRawAny
import org.omniai.sdk.domain.common.json.toRawMap
import org.omniai.sdk.domain.common.json.toJsonObject
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
import org.omniai.sdk.domain.responses.ResponseErrored
import org.omniai.sdk.domain.responses.ResponseStarted
import org.omniai.sdk.domain.responses.TextDeltaEvent
import org.omniai.sdk.domain.responses.ToolCallArgumentsDeltaEvent
import org.omniai.sdk.domain.responses.ToolCallStartedEvent
import org.omniai.sdk.domain.responses.UsageReported

class AnthropicOutboundTranslator : OutboundTranslator<AnthropicMessagesRequest, AnthropicMessageResponse, AnthropicStreamEvent> {


    /*
     This version of functions doesn't treat the JsonSchema , it needs to add a new message for that
     */
    override fun fromDomain(domainRequest: CommonRequest): AnthropicMessagesRequest {

        val providerOptions = domainRequest.providerOptions

        return AnthropicMessagesRequest(
            model = domainRequest.model,
            maxTokens = domainRequest.config?.maxTokens ?: 1024,
            messages = domainRequest.messages.map(CommonRequestMessage::toAnthropicMessageInput),
            system = domainRequest.systemPrompt?.text?.takeIf { it.isNotBlank() }
                ?.let(::RawText),
            tools = domainRequest.tools.map(CommonTool::toAnthropicToolDefinition).ifEmpty { null },
            toolChoice = domainRequest.toolChoice?.toAnthropicToolChoice(),
            stream = providerOptions.get<Boolean>("stream"),
            temperature = domainRequest.config?.temperature,
            topP = domainRequest.config?.topP,
            topK = providerOptions.get<Int>("topK"),
            stopSequences = domainRequest.config?.stopSequences,
            stopToken = providerOptions.get<String>("stopToken"),
            thinking = providerOptions.get<AnthropicThinkingConfig>("thinking"),
            metadata = providerOptions.get<Map<String, Any?>>("metadata")  // criar type alias para isto
        )
    }

    override fun toDomain(providerResponse: AnthropicMessageResponse): CommonResponse {
        val message = CommonResponseMessage(
            role = providerResponse.role.toCommonRole(),
            content = providerResponse.content.mapNotNull(::toDomainContentPart)
        )

        return CommonResponse(
            provider = Provider.ANTHROPIC,
            id = providerResponse.id,
            model = providerResponse.model,
            choices = listOf(
                CommonChoice(
                    index = 0,
                    message = message,
                    finishReason = providerResponse.stopReason.toDomainFinishReason()
                )
            ),
            usage = providerResponse.usage?.toDomainUsage()
        )
    }

    override fun toDomainEvent(providerEvent: AnthropicStreamEvent): CommonResponseEvent =
        when (providerEvent) {
            is AnthropicStreamEvent.MessageStart -> ResponseStarted(
                provider = Provider.ANTHROPIC,
                id = providerEvent.message.id,
                model = Model(providerEvent.message.model),
                sequence = 0,
                providerEventType = providerEvent.type
            )
            is AnthropicStreamEvent.ContentBlockStart -> when (val block = providerEvent.contentBlock) {
                is AnthropicOutputContent.Text -> ChoiceStarted(
                    provider = Provider.ANTHROPIC,
                    id = null,
                    model = Model(""),
                    sequence = providerEvent.index.toLong(),
                    choiceIndex = providerEvent.index,
                    role = CommonRole.ASSISTANT,
                    providerEventType = providerEvent.type
                )
                is AnthropicOutputContent.ToolUse -> ToolCallStartedEvent(
                    provider = Provider.ANTHROPIC,
                    id = null,
                    model = Model(""),
                    sequence = providerEvent.index.toLong(),
                    choiceIndex = 0,
                    toolCallIndex = providerEvent.index,
                    toolCallId = block.id,
                    functionName = block.name,
                    providerEventType = providerEvent.type
                )
                is AnthropicOutputContent.Thinking -> ChoiceStarted(
                    provider = Provider.ANTHROPIC,
                    id = null,
                    model = Model(""),
                    sequence = providerEvent.index.toLong(),
                    choiceIndex = providerEvent.index,
                    role = CommonRole.ASSISTANT,
                    providerEventType = providerEvent.type
                )
            }
            is AnthropicStreamEvent.ContentBlockDelta -> when (val delta = providerEvent.delta) {
                is AnthropicStreamDelta.TextDelta -> TextDeltaEvent(
                    provider = Provider.ANTHROPIC,
                    id = null,
                    model = Model(""),
                    sequence = providerEvent.index.toLong(),
                    choiceIndex = providerEvent.index,
                    text = delta.text,
                    providerEventType = providerEvent.type
                )
                is AnthropicStreamDelta.InputJsonDelta -> ToolCallArgumentsDeltaEvent(
                    provider = Provider.ANTHROPIC,
                    id = null,
                    model = Model(""),
                    sequence = providerEvent.index.toLong(),
                    choiceIndex = 0,
                    toolCallIndex = providerEvent.index,
                    argumentsFragment = delta.partialJson,
                    providerEventType = providerEvent.type
                )
                is AnthropicStreamDelta.SignatureDelta -> TextDeltaEvent(
                    provider = Provider.ANTHROPIC,
                    id = null,
                    model = Model(""),
                    sequence = providerEvent.index.toLong(),
                    choiceIndex = providerEvent.index,
                    text = delta.signature,
                    providerEventType = providerEvent.type
                )
                is AnthropicStreamDelta.ThinkingDelta -> TextDeltaEvent(
                    provider = Provider.ANTHROPIC,
                    id = null,
                    model = Model(""),
                    sequence = providerEvent.index.toLong(),
                    choiceIndex = providerEvent.index,
                    text = delta.thinking,
                    providerEventType = providerEvent.type
                )
            }
            is AnthropicStreamEvent.ContentBlockStop -> ChoiceFinished(
                provider = Provider.ANTHROPIC,
                id = null,
                model = Model(""),
                sequence = providerEvent.index.toLong(),
                choiceIndex = providerEvent.index,
                providerEventType = providerEvent.type
            )
            is AnthropicStreamEvent.MessageDelta -> {
                val usage = providerEvent.usage
                if (usage != null) {
                    UsageReported(
                        provider = Provider.ANTHROPIC,
                        id = null,
                        model = Model(""),
                        sequence = 0,
                        usage = usage.toDomainUsage(),
                        providerEventType = providerEvent.type
                    )
                } else {
                    ChoiceFinished(
                        provider = Provider.ANTHROPIC,
                        id = null,
                        model = Model(""),
                        sequence = 0,
                        choiceIndex = 0,
                        finishReason = providerEvent.delta.stopReason.toDomainFinishReason(),
                        providerEventType = providerEvent.type
                    )
                }
            }
            is AnthropicStreamEvent.MessageStop -> ResponseCompleted(
                provider = Provider.ANTHROPIC,
                id = null,
                model = Model(""),
                sequence = 0,
                providerEventType = providerEvent.type
            )
            is AnthropicStreamEvent.Error -> ResponseErrored(
                provider = Provider.ANTHROPIC,
                id = null,
                model = Model(""), // tornar model um sealed class no futuro
                sequence = 0,
                message = providerEvent.error.message,
                retryable = providerEvent.error.type.lowercase().contains("overloaded"),
                providerEventType = providerEvent.type
            )
            is AnthropicStreamEvent.Ping -> ResponseStarted(
                provider = Provider.ANTHROPIC,
                id = null,
                model = Model(""),
                sequence = 0,
                providerEventType = providerEvent.type
            )
        }

}

private fun CommonRequestMessage.toAnthropicMessageInput(): AnthropicMessageInput =
    AnthropicMessageInput(
        role = role.toAnthropicRole(),
        content = content.toAnthropicContent()
    )

private fun List<RequestContentPart>.toAnthropicContent(): AnthropicContent {
    if (size == 1) {
        val first = first()
        if (first is TextPart) return RawText(first.text)
    }

    return ListContentBlock(
        blocks = mapNotNull { part ->
            when (part) {
                is TextPart -> AnthropicInputContentBlock.Text(text = part.text)
                is ToolCallPart -> AnthropicInputContentBlock.ToolUse(
                    id = part.toolCallId,
                    name = part.functionName,
                    input = JsonValue.JsonObject(part.argumentsJson).toRawMap()
                )
                is ToolResultPart -> AnthropicInputContentBlock.ToolResult(
                    toolUseId = part.toolCallId,
                    content = part.content.firstOrNull()?.toRawAny()?.toString().orEmpty()
                )
                is JsonPart -> AnthropicInputContentBlock.Text(text = part.json.toString())
            }
        }
    )
}

private fun CommonTool.toAnthropicToolDefinition(): AnthropicToolDefinition =
    AnthropicToolDefinition(
        name = name,
        description = description,
        inputSchema = JsonValue.JsonObject(parametersSchema).toRawMap()
    )

private fun ToolChoice.toAnthropicToolChoice(): AnthropicToolChoice =
    when (this) {
        ToolChoice.Auto -> AnthropicToolChoice(type = "auto")
        ToolChoice.None -> AnthropicToolChoice(type = "none")
        ToolChoice.Required -> AnthropicToolChoice(type = "any")
        is ToolChoice.Specific -> AnthropicToolChoice(type = "tool", name = toolNames.firstOrNull())
    }

private fun String.toCommonRole(): CommonRole =
    when (lowercase()) {
        "user" -> CommonRole.USER
        "assistant" -> CommonRole.ASSISTANT
        "tool" -> CommonRole.TOOL
        "system" -> CommonRole.SYSTEM
        else -> CommonRole.USER
    }

private fun toDomainContentPart(part: AnthropicOutputContent): ResponseContentPart? =
    when (part) {
        is AnthropicOutputContent.Text -> TextPart(part.text)
        is AnthropicOutputContent.ToolUse -> ToolCallPart(
            toolCallId = part.id,
            functionName = part.name,
            argumentsJson = part.input.orEmpty().toJsonObject().properties
        )
        is AnthropicOutputContent.Thinking -> RefusalPart(reason = part.thinking)
    }

private fun AnthropicUsage.toDomainUsage(): CommonUsage =
    CommonUsage(
        inputTokens = inputTokens,
        outputTokens = outputTokens,
        totalTokens = (inputTokens ?: 0) + (outputTokens ?: 0)
    )

private fun String?.toDomainFinishReason(): FinishReason? =
    when (this) {
        "end_turn" -> FinishReason.STOP
        "max_tokens" -> FinishReason.LENGTH
        "tool_use" -> FinishReason.TOOL_CALL
        "stop_sequence" -> FinishReason.CONTENT_FILTER
        null -> null
        else -> FinishReason.OTHER
    }

private fun CommonRole.toAnthropicRole(): String =
    when (this) {
        CommonRole.SYSTEM -> "assistant"
        CommonRole.USER -> "user"
        CommonRole.ASSISTANT -> "assistant"
        CommonRole.TOOL -> "assistant"
    }

