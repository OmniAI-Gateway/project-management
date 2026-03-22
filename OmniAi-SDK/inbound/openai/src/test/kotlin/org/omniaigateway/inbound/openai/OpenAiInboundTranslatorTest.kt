package org.omniaigateway.inbound.openai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.omniaigateway.contracts.openai.input.OpenAiChatCompletionsRequest
import org.omniaigateway.contracts.openai.input.OpenAiFunctionDefinition
import org.omniaigateway.contracts.openai.input.OpenAiMessageInput
import org.omniaigateway.contracts.openai.input.OpenAiResponseFormat
import org.omniaigateway.contracts.openai.input.OpenAiTool
import org.omniaigateway.contracts.openai.input.OpenAiToolCall
import org.omniaigateway.contracts.openai.input.OpenAiToolCallFunction
import org.omniaigateway.contracts.openai.input.OpenAiToolChoice
import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.Model
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.common.content.ToolCallPart
import org.omniaigateway.domain.common.json.JsonValue
import org.omniaigateway.domain.responses.CommonChoice
import org.omniaigateway.domain.responses.CommonResponse
import org.omniaigateway.domain.responses.CommonResponseMessage
import org.omniaigateway.domain.responses.CommonUsage
import org.omniaigateway.domain.responses.FinishReason
import org.omniaigateway.domain.responses.ResponseStarted
import org.omniaigateway.domain.responses.ToolCallStartedEvent
import org.omniaigateway.domain.responses.UsageReported

class OpenAiInboundTranslatorTest {

    private val translator = OpenAiInboundTranslator()

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

        val domain = translator.toDomain(request)

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
        val domainResponse = CommonResponse(
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

        val response = translator.fromDomain(domainResponse)

        assertEquals("chatcmpl_123", response.id)
        assertEquals("chat.completion", response.obj)
        assertEquals("tool_calls", response.choices.first().finishReason)
        assertEquals("assistant", response.choices.first().message?.role)
        assertEquals("Done", response.choices.first().message?.content)
        assertEquals("Lisbon", response.choices.first().message?.toolCalls?.first()?.function?.arguments?.get("city"))
        assertEquals(10, response.usage?.promptTokens)
        assertEquals(20, response.usage?.completionTokens)
    }

    @Test
    fun `maps common stream events to openai response chunks`() {
        val started = translator.fromDomainEvent(
            ResponseStarted(
                provider = Provider.OPENAI,
                id = "chatcmpl_abc",
                model = Model("gpt-4o-mini"),
                sequence = 1
            )
        )
        assertEquals("chat.completion.chunk", started.obj)
        assertEquals("chatcmpl_abc", started.id)

        val toolCallStarted = translator.fromDomainEvent(
            ToolCallStartedEvent(
                provider = Provider.OPENAI,
                id = "chatcmpl_abc",
                model = Model("gpt-4o-mini"),
                sequence = 2,
                choiceIndex = 0,
                toolCallIndex = 0,
                toolCallId = "call_1",
                functionName = "weather"
            )
        )
        assertEquals("weather", toolCallStarted.choices.first().delta?.toolCalls?.first()?.function?.name)

        val usage = translator.fromDomainEvent(
            UsageReported(
                provider = Provider.OPENAI,
                id = "chatcmpl_abc",
                model = Model("gpt-4o-mini"),
                sequence = 3,
                usage = CommonUsage(inputTokens = 3, outputTokens = 4, totalTokens = 7)
            )
        )
        assertEquals(3, usage.usage?.promptTokens)
        assertEquals(4, usage.usage?.completionTokens)
        assertEquals(7, usage.usage?.totalTokens)
    }
}

