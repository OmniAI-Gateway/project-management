package org.omniaigateway.inbound.anthropic

import org.omniaigateway.contracts.anthropic.input.AnthropicContent
import org.omniaigateway.contracts.anthropic.input.AnthropicInputContentBlock
import org.omniaigateway.contracts.anthropic.input.AnthropicMessageInput
import org.omniaigateway.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniaigateway.contracts.anthropic.input.AnthropicToolChoice
import org.omniaigateway.contracts.anthropic.input.AnthropicToolDefinition
import org.omniaigateway.contracts.anthropic.input.ListContentBlock
import org.omniaigateway.contracts.anthropic.input.RawText
import org.omniaigateway.contracts.anthropic.output.AnthropicError
import org.omniaigateway.contracts.anthropic.output.AnthropicMessageResponse
import org.omniaigateway.contracts.anthropic.output.AnthropicOutputContent
import org.omniaigateway.contracts.anthropic.output.AnthropicStreamDelta
import org.omniaigateway.contracts.anthropic.output.AnthropicStreamEvent
import org.omniaigateway.contracts.anthropic.output.AnthropicUsage
import org.omniaigateway.contracts.anthropic.output.MessageDeltaInfo
import org.omniaigateway.core.ports.InboundTranslator
import org.omniaigateway.domain.common.CommonGenerationConfig
import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.CommonTool
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.common.SystemPrompt
import org.omniaigateway.domain.common.ToolChoice
import org.omniaigateway.domain.common.content.JsonPart
import org.omniaigateway.domain.common.content.RequestContentPart
import org.omniaigateway.domain.common.content.RefusalPart
import org.omniaigateway.domain.common.content.ResponseContentPart
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.common.content.ToolCallPart
import org.omniaigateway.domain.common.content.ToolResultPart
import org.omniaigateway.domain.common.json.toJsonObject
import org.omniaigateway.domain.common.json.toRawMap
import org.omniaigateway.domain.common.json.toJsonValue
import org.omniaigateway.domain.requests.CommonRequest
import org.omniaigateway.domain.requests.CommonRequestMessage
import org.omniaigateway.domain.responses.ChoiceFinished
import org.omniaigateway.domain.responses.ChoiceStarted
import org.omniaigateway.domain.responses.CommonResponse
import org.omniaigateway.domain.responses.CommonResponseEvent
import org.omniaigateway.domain.responses.CommonUsage
import org.omniaigateway.domain.responses.FinishReason
import org.omniaigateway.domain.responses.ResponseCompleted
import org.omniaigateway.domain.responses.ResponseErrored
import org.omniaigateway.domain.responses.ResponseStarted
import org.omniaigateway.domain.responses.TextDeltaEvent
import org.omniaigateway.domain.responses.ToolCallArgumentsDeltaEvent
import org.omniaigateway.domain.responses.ToolCallStartedEvent
import org.omniaigateway.domain.responses.UsageReported

class AnthropicInboundTranslator : InboundTranslator<AnthropicMessagesRequest, AnthropicMessageResponse, AnthropicStreamEvent> {
	override val provider: Provider = Provider.ANTHROPIC

	override fun toDomain(clientRequest: AnthropicMessagesRequest): CommonRequest {
		val providerOptions = buildMap<String, Any?> {
			if (clientRequest.stream != null) put("stream", clientRequest.stream)
			if (clientRequest.topK != null) put("topK", clientRequest.topK)
			if (clientRequest.stopToken != null) put("stopToken", clientRequest.stopToken)
			if (clientRequest.thinking != null) put("thinking", clientRequest.thinking)
			if (clientRequest.metadata != null) put("metadata", clientRequest.metadata)
		}

		return CommonRequest(
			provider = provider,
			model = clientRequest.model,
			messages = clientRequest.messages.map(AnthropicMessageInput::toDomainMessage),
			systemPrompt = clientRequest.system?.takeIf(String::isNotBlank)?.let(::SystemPrompt),
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
			id = domainResponse.id ?: "",
			type = "message",
			role = firstChoice?.message?.role?.toAnthropicRole() ?: "assistant",
			model = domainResponse.model,
			content = firstChoice?.message?.content?.mapNotNull(::toAnthropicContentPart).orEmpty(),
			stopReason = firstChoice?.finishReason.toAnthropicStopReason(),
			usage = domainResponse.usage?.toAnthropicUsage()
		)
	}

	override fun fromDomainEvent(domainEvent: CommonResponseEvent): AnthropicStreamEvent =
		when (domainEvent) {
			is ResponseStarted -> AnthropicStreamEvent.MessageStart(
				message = AnthropicMessageResponse(
					id = domainEvent.id ?: "",
					type = "message",
					role = "assistant",
					model = domainEvent.model.model
				)
			)
			is ChoiceStarted -> AnthropicStreamEvent.ContentBlockStart(
				index = domainEvent.choiceIndex,
				contentBlock = AnthropicOutputContent.Text(text = "")
			)
			is TextDeltaEvent -> AnthropicStreamEvent.ContentBlockDelta(
				index = domainEvent.choiceIndex,
				delta = AnthropicStreamDelta.TextDelta(text = domainEvent.text)
			)
			is ToolCallStartedEvent -> AnthropicStreamEvent.ContentBlockStart(
				index = domainEvent.toolCallIndex,
				contentBlock = AnthropicOutputContent.ToolUse(
					id = domainEvent.toolCallId,
					name = domainEvent.functionName,
					input = emptyMap()
				)
			)
			is ToolCallArgumentsDeltaEvent -> AnthropicStreamEvent.ContentBlockDelta(
				index = domainEvent.toolCallIndex,
				delta = AnthropicStreamDelta.InputJsonDelta(partialJson = domainEvent.argumentsFragment)
			)
			is ChoiceFinished -> AnthropicStreamEvent.MessageDelta(
				delta = MessageDeltaInfo(stopReason = domainEvent.finishReason.toAnthropicStopReason())
			)
			is UsageReported -> AnthropicStreamEvent.MessageDelta(
				delta = MessageDeltaInfo(),
				usage = domainEvent.usage.toAnthropicUsage()
			)
			is ResponseCompleted -> AnthropicStreamEvent.MessageStop()
			is ResponseErrored -> AnthropicStreamEvent.Error(
				error = AnthropicError(
					type = if (domainEvent.retryable) "overloaded_error" else "api_error",
					message = domainEvent.message
				)
			)
		}
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
			argumentsJson = input.orEmpty().toJsonObject().properties
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
		parametersSchema = inputSchema.toJsonObject().properties
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
			input = org.omniaigateway.domain.common.json.JsonValue.JsonObject(part.argumentsJson).toRawMap()
		)
		is JsonPart -> null
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

