package org.omniai.sdk.inbound.anthropic

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import org.omniai.sdk.contracts.anthropic.input.AnthropicContent
import org.omniai.sdk.contracts.anthropic.input.AnthropicInputContentBlock
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessageInput
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.input.AnthropicToolChoice
import org.omniai.sdk.contracts.anthropic.input.AnthropicToolDefinition
import org.omniai.sdk.contracts.anthropic.input.ListContentBlock
import org.omniai.sdk.contracts.anthropic.input.RawText
import org.omniai.sdk.contracts.anthropic.output.AnthropicError
import org.omniai.sdk.contracts.anthropic.output.AnthropicMessageResponse
import org.omniai.sdk.contracts.anthropic.output.AnthropicOutputContent
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamDelta
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent
import org.omniai.sdk.contracts.anthropic.output.AnthropicUsage
import org.omniai.sdk.contracts.anthropic.output.MessageDeltaInfo
import org.omniai.sdk.core.commom.TypedMap
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
import org.omniai.sdk.domain.common.json.JsonValue
import org.omniai.sdk.domain.common.json.toDomainJsonObject
import org.omniai.sdk.domain.common.json.toDomainJsonValue
import org.omniai.sdk.domain.common.json.toKotlinxJsonObject
import org.omniai.sdk.domain.common.json.toJsonValue
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.requests.CommonRequestMessage
import org.omniai.sdk.domain.responses.ChoiceFinished
import org.omniai.sdk.domain.responses.ChoiceStarted
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

class AnthropicInboundTranslator : InboundTranslator<AnthropicMessagesRequest, AnthropicMessageResponse, AnthropicStreamEvent> {
	override val provider: Provider = Provider.ANTHROPIC

	override fun toDomain(clientRequest: AnthropicMessagesRequest): CommonRequest {
		val providerOptions = TypedMap().apply {
			clientRequest.stream?.let { put("stream", it) }
			clientRequest.topK?.let { put("topK", it) }
			clientRequest.stopToken?.let { put("stopToken", it) }
			clientRequest.thinking?.let { put("thinking", it) }
			clientRequest.metadata?.let { put("metadata", it) }
		}

		return CommonRequest(
			provider = provider,
			model = clientRequest.model,
			messages = clientRequest.messages.map(AnthropicMessageInput::toDomainMessage),
			systemPrompt = clientRequest.system.toPlainSystemText()?.takeIf(String::isNotBlank)?.let(::SystemPrompt),
			config = CommonGenerationConfig(
				temperature = clientRequest.temperature,
				maxTokens = clientRequest.maxTokens,
				topP = clientRequest.topP,
				stopSequences = clientRequest.stopSequences
			),
			tools = clientRequest.tools?.map(AnthropicToolDefinition::toDomainTool).orEmpty(),
			toolChoice = clientRequest.toolChoice?.toDomainToolChoice(),
			jsonResponse = false,
			providerOptions = providerOptions
		)
	}

	override fun fromDomain(domainResponse: CommonResponse): AnthropicMessageResponse {
		val firstChoice = domainResponse.choices.firstOrNull()

		return AnthropicMessageResponse(
			id = domainResponse.id ?: "", // tratar ids de melhor forma no futuro
			type = "message",
			role = firstChoice?.message?.role?.toAnthropicRole() ?: "assistant",
			model = domainResponse.model,
			content = firstChoice?.message?.content?.mapNotNull(::toAnthropicContentPart).orEmpty(),
			stopReason = firstChoice?.finishReason.toAnthropicStopReason(),
			usage = domainResponse.usage?.toAnthropicUsage()
		)
	}

	override fun fromDomainEvent(domainEvent: Flow<CommonResponseEvent>): Flow<AnthropicStreamEvent> =
		domainEvent.map(CommonResponseEvent::toAnthropicStreamEvent)
}

private fun CommonResponseEvent.toAnthropicStreamEvent(): AnthropicStreamEvent =
	when (this) {
		is ResponseStarted -> AnthropicStreamEvent.MessageStart(
			message = AnthropicMessageResponse(
				id = id ?: "",
				type = "message",
				role = "assistant",
				model = model.model
			)
		)
		is ChoiceStarted -> AnthropicStreamEvent.ContentBlockStart(
			index = choiceIndex,
			contentBlock = AnthropicOutputContent.Text(text = "")
		)
		is TextDeltaEvent -> AnthropicStreamEvent.ContentBlockDelta(
			index = choiceIndex,
			delta = AnthropicStreamDelta.TextDelta(text = text)
		)
		is ToolCallStartedEvent -> AnthropicStreamEvent.ContentBlockStart(
			index = toolCallIndex,
			contentBlock = AnthropicOutputContent.ToolUse(
				id = toolCallId,
				name = functionName,
				input = JsonObject(emptyMap())
			)
		)
		is ToolCallArgumentsDeltaEvent -> AnthropicStreamEvent.ContentBlockDelta(
			index = toolCallIndex,
			delta = AnthropicStreamDelta.InputJsonDelta(partialJson = argumentsFragment)
		)
		is ChoiceFinished -> AnthropicStreamEvent.MessageDelta(
			delta = MessageDeltaInfo(stopReason = finishReason.toAnthropicStopReason())
		)
		is UsageReported -> AnthropicStreamEvent.MessageDelta(
			delta = MessageDeltaInfo(),
			usage = usage.toAnthropicUsage()
		)
		is ResponseCompleted -> AnthropicStreamEvent.MessageStop
		is ResponseErrored -> AnthropicStreamEvent.Error(
			error = AnthropicError(
				type = if (retryable) "overloaded_error" else "api_error",
				message = message
			)
		)
	}

private fun AnthropicContent?.toPlainSystemText(): String? = when (this) {
    null -> null
    is RawText -> text
    is ListContentBlock -> blocks
        .filterIsInstance<AnthropicInputContentBlock.Text>()
        .joinToString("\n") { it.text }
        .takeIf { it.isNotBlank() }
}


private fun String.toCommonRole(): CommonRole =
	when (lowercase()) {
		"user" -> CommonRole.USER
		"assistant" -> CommonRole.ASSISTANT
		"tool" -> CommonRole.TOOL
		"system" -> CommonRole.SYSTEM
		else -> CommonRole.USER
	}

private fun AnthropicInputContentBlock.toDomainPart(index: Int): RequestContentPart? =
	when (this) {
		is AnthropicInputContentBlock.Text -> TextPart(text)
		is AnthropicInputContentBlock.ToolUse -> ToolCallPart(
			toolCallId = id ?: "anthropic-tool-use-$index",
			functionName = name,
			argumentsJson = input?.toDomainJsonObject()?.properties.orEmpty()
		)
		is AnthropicInputContentBlock.ToolResult -> ToolResultPart(
			toolCallId = toolUseId,
			content = listOf(content.toJsonValue())
		)
		is AnthropicInputContentBlock.Thinking -> null
	}

private fun AnthropicContent.toDomainParts(): List<RequestContentPart> =
	when (this) {
		is RawText -> listOf(TextPart(text))
		is ListContentBlock -> blocks.mapIndexedNotNull { index, block -> block.toDomainPart(index) }
	}

private fun AnthropicMessageInput.toDomainMessage(): CommonRequestMessage =
	CommonRequestMessage(
		role = role.toCommonRole(),
		content = content.toDomainParts()
	)

private fun AnthropicToolDefinition.toDomainTool(): CommonTool =
	CommonTool(
		name = name,
		description = description,
		parametersSchema = (inputSchema.toDomainJsonValue() as? JsonValue.JsonObject)?.properties.orEmpty()
	)

private fun AnthropicToolChoice.toDomainToolChoice(): ToolChoice? =
	type.toToolChoice(name)

private fun String.toToolChoice(name: String?): ToolChoice? =
	when (lowercase()) {
		"auto" -> ToolChoice.Auto
		"none" -> ToolChoice.None
		"any" -> ToolChoice.Required
		"tool" -> name?.let(ToolChoice::Specific)
		else -> null
	}

private fun toAnthropicContentPart(part: ResponseContentPart): AnthropicOutputContent? =
	when (part) {
		is TextPart -> AnthropicOutputContent.Text(text = part.text)
		is ToolCallPart -> AnthropicOutputContent.ToolUse(
			id = part.toolCallId,
			name = part.functionName,
			input = JsonValue.JsonObject(part.argumentsJson).toKotlinxJsonObject()
		)
		is JsonPart -> AnthropicOutputContent.Text(text = part.json.toJsonString())
		is RefusalPart -> null
	}

private fun CommonRole.toAnthropicRole(): String =
	when (this) {
		CommonRole.SYSTEM -> "assistant"
		CommonRole.USER -> "user"
		CommonRole.ASSISTANT -> "assistant"
		CommonRole.TOOL -> "assistant"
	}

private fun CommonUsage.toAnthropicUsage(): AnthropicUsage = AnthropicUsage(
	inputTokens = inputTokens,
	outputTokens = outputTokens
)

private fun FinishReason?.toAnthropicStopReason(): String? =
	when (this) {
		FinishReason.STOP -> "end_turn"
		FinishReason.LENGTH -> "max_tokens"
		FinishReason.TOOL_CALL -> "tool_use"
		FinishReason.CONTENT_FILTER -> "stop_sequence"
		FinishReason.OTHER, null -> null
	}
