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
import org.omniaigateway.domain.common.Provider

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
}

