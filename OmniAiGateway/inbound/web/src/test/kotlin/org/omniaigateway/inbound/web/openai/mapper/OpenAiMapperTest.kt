package org.omniaigateway.inbound.web.openai.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.common.content.ToolCallPart
import org.omniaigateway.domain.common.json.JsonValue
import org.omniaigateway.domain.responses.CommonChoice
import org.omniaigateway.domain.responses.CommonResponse
import org.omniaigateway.domain.responses.CommonResponseMessage
import org.omniaigateway.domain.responses.CommonUsage
import org.omniaigateway.domain.responses.FinishReason
import org.omniaigateway.inbound.web.openai.dto.input.OpenAiChatCompletionsRequest
import org.omniaigateway.inbound.web.openai.dto.input.OpenAiFunctionDefinition
import org.omniaigateway.inbound.web.openai.dto.input.OpenAiMessageInput
import org.omniaigateway.inbound.web.openai.dto.input.OpenAiResponseFormat
import org.omniaigateway.inbound.web.openai.dto.input.OpenAiTool
import org.omniaigateway.inbound.web.openai.dto.input.OpenAiToolCall
import org.omniaigateway.inbound.web.openai.dto.input.OpenAiToolCallFunction
import org.omniaigateway.inbound.web.openai.dto.input.OpenAiToolChoice
import org.omniaigateway.inbound.web.openai.dto.output.OpenAiChatCompletionsResponse

class OpenAiMapperTest {

    @Test
    fun `maps openai request to common request`() {
        val request = OpenAiChatCompletionsRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                OpenAiMessageInput(role = "user", content = "hello"),
                OpenAiMessageInput(
                    role = "assistant",
                    toolCalls = listOf(
                        OpenAiToolCall(
                            id = "call_1",
                            function = OpenAiToolCallFunction(
                                name = "weather",
                                arguments = "{\"city\":\"Lisbon\"}"
                            )
                        )
                    )
                )
            ),
            tools = listOf(
                OpenAiTool(
                    function = OpenAiFunctionDefinition(
                        name = "weather",
                        description = "Get weather",
                        parameters = mapOf("type" to "object")
                    )
                )
            ),
            toolChoice = OpenAiToolChoice.Mode("required"),
            responseFormat = OpenAiResponseFormat(type = "json_object"),
            stream = true,
            user = "abc-123"
        )

        val domain = request.toDomain()

        assertEquals(Provider.OPENAI, domain.provider)
        assertEquals("gpt-4o-mini", domain.model)
        assertEquals(2, domain.messages.size)
        assertEquals(CommonRole.USER, domain.messages.first().role)
        assertEquals("weather", domain.tools.first().name)
        assertNotNull(domain.toolChoice)
        assertTrue(domain.jsonResponse)
        assertEquals(true, domain.providerOptions["stream"])
    }

    @Test
    fun `maps common response to openai response`() {
        val response = CommonResponse(
            provider = Provider.OPENAI,
            id = "chatcmpl_123",
            model = "gpt-4o-mini",
            choices = listOf(
                CommonChoice(
                    index = 0,
                    message = CommonResponseMessage(
                        role = CommonRole.ASSISTANT,
                        content = listOf(
                            TextPart("Done"),
                            ToolCallPart(
                                toolCallId = "call_1",
                                functionName = "weather",
                                argumentsJson = mapOf("city" to JsonValue.JsonString("Lisbon"))
                            )
                        )
                    ),
                    finishReason = FinishReason.TOOL_CALL
                )
            ),
            usage = CommonUsage(inputTokens = 10, outputTokens = 20, totalTokens = 30)
        )

        val openAi = OpenAiChatCompletionsResponse.fromDomain(response)

        assertEquals("chatcmpl_123", openAi.id)
        assertEquals("chat.completion", openAi.`object`)
        assertEquals("tool_calls", openAi.choices.first().finishReason)
        assertEquals("assistant", openAi.choices.first().message?.role)
        assertEquals("Done", openAi.choices.first().message?.content)
        assertEquals(1, openAi.choices.first().message?.toolCalls?.size)
        assertEquals("Lisbon", openAi.choices.first().message?.toolCalls?.first()?.function?.arguments?.get("city"))
        assertEquals(10, openAi.usage?.promptTokens)
        assertEquals(20, openAi.usage?.completionTokens)
    }
}

