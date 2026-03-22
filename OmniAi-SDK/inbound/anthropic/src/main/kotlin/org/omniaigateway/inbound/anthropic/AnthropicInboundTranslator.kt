package org.omniaigateway.inbound.anthropic

import org.omniaigateway.contracts.anthropic.input.AnthropicContent
import org.omniaigateway.contracts.anthropic.input.AnthropicInputContentBlock
import org.omniaigateway.contracts.anthropic.input.AnthropicMessageInput
import org.omniaigateway.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniaigateway.contracts.anthropic.input.AnthropicToolChoice
import org.omniaigateway.contracts.anthropic.input.AnthropicToolDefinition
import org.omniaigateway.contracts.anthropic.input.ListContentBlock
import org.omniaigateway.contracts.anthropic.input.RawText
import org.omniaigateway.core.ports.InboundTranslator
import org.omniaigateway.domain.common.CommonGenerationConfig
import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.CommonTool
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.common.SystemPrompt
import org.omniaigateway.domain.common.ToolChoice
import org.omniaigateway.domain.common.content.RequestContentPart
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.common.content.ToolCallPart
import org.omniaigateway.domain.common.content.ToolResultPart
import org.omniaigateway.domain.common.json.toJsonObject
import org.omniaigateway.domain.common.json.toJsonValue
import org.omniaigateway.domain.requests.CommonRequest
import org.omniaigateway.domain.requests.CommonRequestMessage

class AnthropicInboundTranslator : InboundTranslator<AnthropicMessagesRequest> {
	override val provider: Provider = Provider.ANTHROPIC

	override fun toDomain(payload: AnthropicMessagesRequest): CommonRequest {
		val providerOptions = buildMap<String, Any?> {
			if (payload.stream != null) put("stream", payload.stream)
			if (payload.topK != null) put("topK", payload.topK)
			if (payload.stopToken != null) put("stopToken", payload.stopToken)
			if (payload.thinking != null) put("thinking", payload.thinking)
			if (payload.metadata != null) put("metadata", payload.metadata)
		}

		return CommonRequest(
			provider = provider,
			model = payload.model,
			messages = payload.messages.map(AnthropicMessageInput::toDomainMessage),
			systemPrompt = payload.system?.takeIf(String::isNotBlank)?.let(::SystemPrompt),
			config = CommonGenerationConfig(
				temperature = payload.temperature,
				maxTokens = payload.maxTokens,
				topP = payload.topP,
				stopSequences = payload.stopSequences
			),
			tools = payload.tools?.map(AnthropicToolDefinition::toDomainTool).orEmpty(),
			toolChoice = payload.toolChoice?.toDomainToolChoice(),
			jsonResponse = false,
			providerOptions = providerOptions
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

