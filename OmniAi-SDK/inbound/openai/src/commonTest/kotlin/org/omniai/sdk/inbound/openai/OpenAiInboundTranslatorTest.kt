package org.omniai.sdk.inbound.openai

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.omniai.sdk.contracts.openai.input.OpenAiChatCompletionsRequest
import org.omniai.sdk.contracts.openai.input.OpenAiFunctionDefinition
import org.omniai.sdk.contracts.openai.input.OpenAiMessageInput
import org.omniai.sdk.contracts.openai.input.OpenAiResponseFormat
import org.omniai.sdk.contracts.openai.input.OpenAiTool
import org.omniai.sdk.contracts.openai.input.OpenAiToolCall
import org.omniai.sdk.contracts.openai.input.OpenAiToolCallFunction
import org.omniai.sdk.contracts.openai.input.OpenAiToolChoice
import org.omniai.sdk.domain.common.CommonRole
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.common.content.TextPart
import org.omniai.sdk.domain.common.content.ToolCallPart
import org.omniai.sdk.domain.common.json.JsonValue
import org.omniai.sdk.domain.responses.CommonChoice
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseMessage
import org.omniai.sdk.domain.responses.CommonUsage
import org.omniai.sdk.domain.responses.FinishReason
import org.omniai.sdk.domain.responses.ResponseStarted
import org.omniai.sdk.domain.responses.ToolCallArgumentsDeltaEvent
import org.omniai.sdk.domain.responses.ToolCallStartedEvent
import org.omniai.sdk.domain.responses.UsageReported

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
                        parameters = JsonObject(mapOf("type" to JsonPrimitive("object")))
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
        assertEquals("{\"city\":\"Lisbon\"}", response.choices.first().message?.toolCalls?.first()?.function?.arguments)
        assertEquals(10, response.usage?.promptTokens)
        assertEquals(20, response.usage?.completionTokens)
    }

    @Test
    fun `maps common stream events to openai response chunks`() = runTest {
        val started = translator.fromDomainEvent(
            flowOf(ResponseStarted(
                provider = Provider.OPENAI,
                id = "chatcmpl_abc",
                model = Model("gpt-4o-mini"),
                sequence = 1
            ))
        ).first()
        assertEquals("chat.completion.chunk", started.obj)
        assertEquals("chatcmpl_abc", started.id)

        val toolCallStarted = translator.fromDomainEvent(
            flowOf(ToolCallStartedEvent(
                provider = Provider.OPENAI,
                id = "chatcmpl_abc",
                model = Model("gpt-4o-mini"),
                sequence = 2,
                choiceIndex = 0,
                toolCallIndex = 0,
                toolCallId = "call_1",
                functionName = "weather"
            ))
        ).first()
        assertEquals("weather", toolCallStarted.choices.first().delta?.toolCalls?.first()?.function?.name)
        assertEquals("", toolCallStarted.choices.first().delta?.toolCalls?.first()?.function?.arguments)

        val toolArgsDelta = translator.fromDomainEvent(
            flowOf(ToolCallArgumentsDeltaEvent(
                provider = Provider.OPENAI,
                id = "chatcmpl_abc",
                model = Model("gpt-4o-mini"),
                sequence = 2,
                choiceIndex = 0,
                toolCallIndex = 0,
                argumentsFragment = "{\"city\":\"Lis"
            ))
        ).first()
        assertEquals(
            "{\"city\":\"Lis",
            toolArgsDelta.choices.first().delta?.toolCalls?.first()?.function?.arguments
        )

        val usage = translator.fromDomainEvent(
            flowOf(UsageReported(
                provider = Provider.OPENAI,
                id = "chatcmpl_abc",
                model = Model("gpt-4o-mini"),
                sequence = 3,
                usage = CommonUsage(inputTokens = 3, outputTokens = 4, totalTokens = 7)
            ))
        ).first()
        assertEquals(3, usage.usage?.promptTokens)
        assertEquals(4, usage.usage?.completionTokens)
        assertEquals(7, usage.usage?.totalTokens)
    }
}

