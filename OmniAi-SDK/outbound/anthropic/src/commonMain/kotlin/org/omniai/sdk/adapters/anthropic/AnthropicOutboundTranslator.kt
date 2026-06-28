package org.omniai.sdk.adapters.anthropic

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.runningFold
import kotlinx.serialization.json.JsonElement
import org.omniai.sdk.contracts.anthropic.input.AnthropicContent
import org.omniai.sdk.contracts.anthropic.input.AnthropicInputContentBlock
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessageInput
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.input.AnthropicRole
import org.omniai.sdk.contracts.anthropic.input.AnthropicThinkingConfig
import org.omniai.sdk.contracts.anthropic.input.AnthropicToolChoice
import org.omniai.sdk.contracts.anthropic.input.AnthropicToolDefinition
import org.omniai.sdk.contracts.anthropic.input.ListContentBlock
import org.omniai.sdk.contracts.anthropic.input.RawText
import org.omniai.sdk.contracts.anthropic.output.AnthropicMessageResponse
import org.omniai.sdk.contracts.anthropic.output.AnthropicOutputContent
import org.omniai.sdk.contracts.anthropic.output.AnthropicStopReason
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamDelta
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent
import org.omniai.sdk.contracts.anthropic.output.AnthropicUsage
import org.omniai.sdk.domain.common.CommonRole
import org.omniai.sdk.domain.common.CommonTool
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.common.ToolChoice
import org.omniai.sdk.domain.common.content.JsonPart
import org.omniai.sdk.domain.common.content.RequestContentPart
import org.omniai.sdk.domain.common.content.ResponseContentPart
import org.omniai.sdk.domain.common.content.TextPart
import org.omniai.sdk.domain.common.content.ToolCallPart
import org.omniai.sdk.domain.common.content.ToolResultPart
import org.omniai.sdk.domain.common.json.JsonValue
import org.omniai.sdk.domain.common.json.toDomainJsonObject
import org.omniai.sdk.domain.common.json.toKotlinxJsonObject
import org.omniai.sdk.domain.common.json.toRawAny
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
import org.omniai.sdk.ports.outbound.OutboundTranslator

class AnthropicOutboundTranslator : OutboundTranslator<AnthropicMessagesRequest, AnthropicMessageResponse, AnthropicStreamEvent> {
        /*
     This version of functions doesn't treat the JsonSchema , it needs to add a new message for that
         */
    override fun fromDomain(domainRequest: CommonRequest): AnthropicMessagesRequest {
        val providerOptions = domainRequest.providerOptions

        val systemMessagesText =
            domainRequest.messages
                .filter { it.role == CommonRole.SYSTEM }
                .flatMap { it.content }
                .filterIsInstance<TextPart>()
                .joinToString("\n") { it.text }

        val combinedSystemText =
            listOfNotNull(
                domainRequest.systemPrompt?.text?.takeIf { it.isNotBlank() },
                systemMessagesText.takeIf { it.isNotBlank() },
            ).joinToString("\n\n").takeIf { it.isNotBlank() }

        return AnthropicMessagesRequest(
            model = domainRequest.model,
            maxTokens = domainRequest.config?.maxTokens ?: 1024,
            messages =
                domainRequest.messages
                    .filter { it.role != CommonRole.SYSTEM }
                    .map(CommonRequestMessage::toAnthropicMessageInput),
            system = combinedSystemText?.let(::RawText),
            tools = domainRequest.tools.map(CommonTool::toAnthropicToolDefinition).ifEmpty { null },
            toolChoice = domainRequest.toolChoice?.toAnthropicToolChoice(),
            stream = providerOptions.get<Boolean>("stream"),
            temperature = domainRequest.config?.temperature,
            topP = domainRequest.config?.topP,
            topK = providerOptions.get<Int>("topK"),
            stopSequences = domainRequest.config?.stopSequences,
            thinking = providerOptions.get<AnthropicThinkingConfig>("thinking"),
            metadata = providerOptions.get<JsonElement>("metadata"),
        )
    }

    override fun toDomain(providerResponse: AnthropicMessageResponse): CommonResponse {
        val message =
            CommonResponseMessage(
                role = providerResponse.role.toCommonRole(),
                content = providerResponse.content.mapNotNull(::toDomainContentPart),
            )

        return CommonResponse(
            provider = Provider.ANTHROPIC,
            id = providerResponse.id,
            model = providerResponse.model,
            choices =
                listOf(
                    CommonChoice(
                        index = 0,
                        message = message,
                        finishReason = providerResponse.stopReason.toDomainFinishReason(),
                    ),
                ),
            usage = providerResponse.usage?.toDomainUsage(),
        )
    }

    override fun toDomainEvent(providerEvent: Flow<AnthropicStreamEvent>): Flow<CommonResponseEvent> =
        providerEvent
            .runningFold(AnthropicEventContext()) { context, event ->
                val translatedEvent = event.toDomainStreamEvent(context.id, context.model)
                if (translatedEvent != null) {
                    AnthropicEventContext(translatedEvent.id, translatedEvent.model, translatedEvent)
                } else {
                    context.copy(event = null)
                }
            }.mapNotNull { it.event }
}

private data class AnthropicEventContext(
    val id: String = "",
    val model: Model = Model(""),
    val event: CommonResponseEvent? = null,
)

private fun AnthropicStreamEvent.toDomainStreamEvent(
    receivedId: String,
    receivedModel: Model,
): CommonResponseEvent? =
    when (this) {
        is AnthropicStreamEvent.MessageStart -> {
            ResponseStarted(
                provider = Provider.ANTHROPIC,
                id = message.id,
                model = Model(message.model),
                sequence = 0,
                providerEventType = eventType(),
            )
        }

        is AnthropicStreamEvent.ContentBlockStart -> {
            when (val block = contentBlock) {
                is AnthropicOutputContent.Text -> {
                    ChoiceStarted(
                        provider = Provider.ANTHROPIC,
                        id = receivedId,
                        model = receivedModel,
                        sequence = index.toLong(),
                        choiceIndex = index,
                        role = CommonRole.ASSISTANT,
                        providerEventType = eventType(),
                    )
                }

                is AnthropicOutputContent.ToolUse -> {
                    ToolCallStartedEvent(
                        provider = Provider.ANTHROPIC,
                        id = receivedId,
                        model = receivedModel,
                        sequence = index.toLong(),
                        choiceIndex = 0,
                        toolCallIndex = index,
                        toolCallId = block.id,
                        functionName = block.name,
                        providerEventType = eventType(),
                    )
                }

                is AnthropicOutputContent.Thinking -> {
                    ChoiceStarted(
                        provider = Provider.ANTHROPIC,
                        id = receivedId,
                        model = receivedModel,
                        sequence = index.toLong(),
                        choiceIndex = index,
                        role = CommonRole.ASSISTANT,
                        providerEventType = eventType(),
                    )
                }
            }
        }

        is AnthropicStreamEvent.ContentBlockDelta -> {
            when (val delta = this.delta) {
                is AnthropicStreamDelta.TextDelta -> {
                    TextDeltaEvent(
                        provider = Provider.ANTHROPIC,
                        id = receivedId,
                        model = receivedModel,
                        sequence = index.toLong(),
                        choiceIndex = index,
                        text = delta.text,
                        providerEventType = eventType(),
                    )
                }

                is AnthropicStreamDelta.InputJsonDelta -> {
                    ToolCallArgumentsDeltaEvent(
                        provider = Provider.ANTHROPIC,
                        id = receivedId,
                        model = receivedModel,
                        sequence = index.toLong(),
                        choiceIndex = 0,
                        toolCallIndex = index,
                        argumentsFragment = delta.partialJson,
                        providerEventType = eventType(),
                    )
                }

                is AnthropicStreamDelta.SignatureDelta -> {
                    TextDeltaEvent(
                        provider = Provider.ANTHROPIC,
                        id = receivedId,
                        model = receivedModel,
                        sequence = index.toLong(),
                        choiceIndex = index,
                        text = delta.signature,
                        providerEventType = eventType(),
                    )
                }

                is AnthropicStreamDelta.ThinkingDelta -> {
                    TextDeltaEvent(
                        provider = Provider.ANTHROPIC,
                        id = receivedId,
                        model = receivedModel,
                        sequence = index.toLong(),
                        choiceIndex = index,
                        text = delta.thinking,
                        providerEventType = eventType(),
                    )
                }
            }
        }

        is AnthropicStreamEvent.ContentBlockStop -> {
            ChoiceFinished(
                provider = Provider.ANTHROPIC,
                id = receivedId,
                model = receivedModel,
                sequence = index.toLong(),
                choiceIndex = index,
                providerEventType = eventType(),
            )
        }

        is AnthropicStreamEvent.MessageDelta -> {
            val usage = usage
            if (usage != null) {
                UsageReported(
                    provider = Provider.ANTHROPIC,
                    id = receivedId,
                    model = receivedModel,
                    sequence = 0,
                    usage = usage.toDomainUsage(),
                    providerEventType = eventType(),
                )
            } else {
                ChoiceFinished(
                    provider = Provider.ANTHROPIC,
                    id = receivedId,
                    model = receivedModel,
                    sequence = 0,
                    choiceIndex = 0,
                    finishReason = delta.stopReason.toDomainFinishReason(),
                    providerEventType = eventType(),
                )
            }
        }

        is AnthropicStreamEvent.MessageStop -> {
            ResponseCompleted(
                provider = Provider.ANTHROPIC,
                id = receivedId,
                model = receivedModel,
                sequence = 0,
                providerEventType = eventType(),
            )
        }

        is AnthropicStreamEvent.Error -> {
            ResponseErrored(
                provider = Provider.ANTHROPIC,
                id = receivedId,
                model = receivedModel,
                sequence = 0,
                message = error.message,
                retryable = error.type.lowercase().contains("overloaded"),
                providerEventType = eventType(),
            )
        }

        is AnthropicStreamEvent.Ping -> {
            null
        }
    }

private fun AnthropicStreamEvent.eventType(): String =
    when (this) {
        is AnthropicStreamEvent.MessageStart -> "message_start"
        is AnthropicStreamEvent.ContentBlockStart -> "content_block_start"
        is AnthropicStreamEvent.ContentBlockDelta -> "content_block_delta"
        is AnthropicStreamEvent.ContentBlockStop -> "content_block_stop"
        is AnthropicStreamEvent.MessageDelta -> "message_delta"
        AnthropicStreamEvent.MessageStop -> "message_stop"
        AnthropicStreamEvent.Ping -> "ping"
        is AnthropicStreamEvent.Error -> "error"
    }

private fun CommonRequestMessage.toAnthropicMessageInput(): AnthropicMessageInput =
    AnthropicMessageInput(
        role = role.toAnthropicRole(),
        content = content.toAnthropicContent(),
    )

private fun List<RequestContentPart>.toAnthropicContent(): AnthropicContent {
    if (size == 1) {
        val first = first()
        if (first is TextPart) return RawText(first.text)
    }
    return ListContentBlock(
        blocks =
            mapNotNull { part ->
                when (part) {
                    is TextPart -> {
                        AnthropicInputContentBlock.Text(text = part.text)
                    }

                    is ToolCallPart -> {
                        AnthropicInputContentBlock.ToolUse(
                            id = part.toolCallId,
                            name = part.functionName,
                            input = JsonValue.JsonObject(part.argumentsJson).toKotlinxJsonObject(),
                        )
                    }

                    is ToolResultPart -> {
                        AnthropicInputContentBlock.ToolResult(
                            toolUseId = part.toolCallId,
                            content =
                                part.content
                                    .firstOrNull()
                                    ?.toRawAny()
                                    ?.toString()
                                    .orEmpty(),
                        )
                    }

                    is JsonPart -> {
                        AnthropicInputContentBlock.Text(text = part.json.toString())
                    }
                }
            },
    )
}

private fun CommonTool.toAnthropicToolDefinition(): AnthropicToolDefinition =
    AnthropicToolDefinition(
        name = name,
        description = description,
        inputSchema = JsonValue.JsonObject(parametersSchema).toKotlinxJsonObject(),
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

private fun String?.toDomainFinishReason(): FinishReason? =
    when (this) {
        "end_turn" -> FinishReason.STOP
        "max_tokens" -> FinishReason.LENGTH
        "tool_use" -> FinishReason.TOOL_CALL
        "stop_sequence" -> FinishReason.CONTENT_FILTER
        null -> null
        else -> FinishReason.OTHER // É uma boa prática ter um fallback caso a Anthropic adicione novos motivos no futuro
    }

private fun toDomainContentPart(part: AnthropicOutputContent): ResponseContentPart? =
    when (part) {
        is AnthropicOutputContent.Text -> {
            TextPart(part.text)
        }

        is AnthropicOutputContent.ToolUse -> {
            ToolCallPart(
                toolCallId = part.id,
                functionName = part.name,
                argumentsJson =
                    part.input
                        ?.toDomainJsonObject()
                        ?.properties
                        .orEmpty(),
            )
        }

        is AnthropicOutputContent.Thinking -> {
            TextPart(text = part.thinking)
        }
    }

private fun AnthropicUsage.toDomainUsage(): CommonUsage =
    CommonUsage(
        inputTokens = inputTokens,
        outputTokens = outputTokens,
        totalTokens = (inputTokens ?: 0) + (outputTokens ?: 0),
    )

private fun AnthropicStopReason?.toDomainFinishReason(): FinishReason? =
    when (this) {
        AnthropicStopReason.END_TURN -> FinishReason.STOP
        AnthropicStopReason.MAX_TOKENS -> FinishReason.LENGTH
        AnthropicStopReason.TOOL_USE -> FinishReason.TOOL_CALL
        AnthropicStopReason.STOP_SEQUENCE -> FinishReason.CONTENT_FILTER
        null -> null
    }

private fun CommonRole.toAnthropicRole(): AnthropicRole =
    when (this) {
        CommonRole.SYSTEM -> AnthropicRole.USER
        CommonRole.USER -> AnthropicRole.USER
        CommonRole.ASSISTANT -> AnthropicRole.ASSISTANT
        CommonRole.TOOL -> AnthropicRole.USER
    }
