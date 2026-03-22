package org.omniaigateway.inbound.openai

import org.omniaigateway.contracts.openai.input.OpenAiChatCompletionsRequest
import org.omniaigateway.contracts.openai.input.OpenAiMessageInput
import org.omniaigateway.contracts.openai.input.OpenAiStop
import org.omniaigateway.contracts.openai.input.OpenAiTool
import org.omniaigateway.contracts.openai.input.OpenAiToolCall
import org.omniaigateway.contracts.openai.input.OpenAiToolChoice
import org.omniaigateway.core.ports.InboundTranslator
import org.omniaigateway.domain.common.CommonGenerationConfig
import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.CommonTool
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.common.ToolChoice
import org.omniaigateway.domain.common.content.RequestContentPart
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.common.content.ToolCallPart
import org.omniaigateway.domain.common.content.ToolResultPart
import org.omniaigateway.domain.common.json.JsonValue
import org.omniaigateway.domain.common.json.toJsonObject
import org.omniaigateway.domain.common.json.toJsonValue
import org.omniaigateway.domain.requests.CommonRequest
import org.omniaigateway.domain.requests.CommonRequestMessage

class OpenAiInboundTranslator : InboundTranslator<OpenAiChatCompletionsRequest> {
    override val provider: Provider = Provider.OPENAI

    override fun toDomain(payload: OpenAiChatCompletionsRequest): CommonRequest {
        val providerOptions = buildMap<String, Any?> {
            if (payload.stream != null) put("stream", payload.stream)
            if (payload.frequencyPenalty != null) put("frequencyPenalty", payload.frequencyPenalty)
            if (payload.presencePenalty != null) put("presencePenalty", payload.presencePenalty)
            if (payload.n != null) put("n", payload.n)
            if (payload.seed != null) put("seed", payload.seed)
            if (payload.user != null) put("user", payload.user)
            if (payload.logitBias != null) put("logitBias", payload.logitBias)
            if (payload.logprobs != null) put("logprobs", payload.logprobs)
            if (payload.topLogprobs != null) put("topLogprobs", payload.topLogprobs)
            if (payload.responseFormat != null) put("responseFormat", payload.responseFormat)
        }

        return CommonRequest(
            provider = provider,
            model = payload.model,
            messages = payload.messages.map(OpenAiMessageInput::toDomainMessage),
            config = CommonGenerationConfig(
                temperature = payload.temperature,
                maxTokens = payload.maxTokens,
                topP = payload.topP,
                stopSequences = payload.stop.toStopSequences()
            ),
            tools = payload.tools?.map(OpenAiTool::toDomainTool).orEmpty(),
            toolChoice = payload.toolChoice?.toDomainToolChoice(),
            jsonResponse = payload.responseFormat?.type.equals("json_object", ignoreCase = true)
                || payload.responseFormat?.type.equals("json_schema", ignoreCase = true),
            providerOptions = providerOptions
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

private fun OpenAiMessageInput.toDomainMessage(): CommonRequestMessage =
    CommonRequestMessage(
        role = role.toCommonRole(),
        content = buildList<RequestContentPart> {
            val rawContent = content
            val rawToolCallId = toolCallId

            content?.takeIf { it.isNotBlank() }?.let { add(TextPart(it)) }
            toolCalls.orEmpty().forEachIndexed { index, toolCall -> add(toolCall.toDomainPart(index)) }
            if (role.equals("tool", ignoreCase = true) && rawToolCallId != null && rawContent != null) {
                add(ToolResultPart(toolCallId = rawToolCallId, content = listOf(rawContent.toJsonValue())))
            }
        }
    )

private fun OpenAiTool.toDomainTool(): CommonTool =
    CommonTool(
        name = function.name,
        description = function.description ?: function.name,
        parametersSchema = function.parameters.orEmpty().toJsonObject().properties
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
