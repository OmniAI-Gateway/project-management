package org.omniaigateway.inbound.web.anthropic.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.common.content.ToolCallPart
import org.omniaigateway.domain.common.content.JsonPart
import org.omniaigateway.domain.common.json.JsonValue
import org.omniaigateway.domain.responses.CommonChoice
import org.omniaigateway.domain.responses.CommonResponse
import org.omniaigateway.domain.responses.CommonResponseMessage
import org.omniaigateway.domain.responses.CommonUsage
import org.omniaigateway.domain.responses.FinishReason
import org.omniaigateway.inbound.web.anthropic.dto.input.AnthropicInputContentBlock
import org.omniaigateway.inbound.web.anthropic.dto.input.AnthropicMessageInput
import org.omniaigateway.inbound.web.anthropic.dto.input.AnthropicMessagesRequest
import org.omniaigateway.inbound.web.anthropic.dto.input.AnthropicToolChoice
import org.omniaigateway.inbound.web.anthropic.dto.input.AnthropicToolDefinition
import org.omniaigateway.inbound.web.anthropic.dto.input.ListContentBlock
import org.omniaigateway.inbound.web.anthropic.dto.output.AnthropicOutputContent
import org.omniaigateway.inbound.web.anthropic.dto.output.AnthropicMessageResponse

class AnthropicMapperTest {

    @Test
    fun `maps anthropic request to common request`() {
        val request = AnthropicMessagesRequest(
            model = "claude-3-5-sonnet",
            maxTokens = 256,
            messages = listOf(
                AnthropicMessageInput(
                    role = "user",
                    content = ListContentBlock(
                        blocks = listOf(
                            AnthropicInputContentBlock.Text(text = "hello"),
                            AnthropicInputContentBlock.ToolUse(
                                id = "call-1",
                                name = "weather",
                                input = mapOf("city" to "Lisbon")
                            )
                        )
                    )
                )
            ),
            tools = listOf(
                AnthropicToolDefinition(
                    name = "weather",
                    description = "Get weather",
                    inputSchema = mapOf("type" to "object")
                )
            ),
            toolChoice = AnthropicToolChoice(type = "tool", name = "weather"),
            temperature = 0.3,
            stream = true,
            metadata = mapOf("traceId" to "abc-123")
        )

        val domain = request.toDomain()

        assertEquals(Provider.ANTHROPIC, domain.provider)
        assertEquals("claude-3-5-sonnet", domain.model)
        assertEquals(1, domain.messages.size)
        assertEquals(CommonRole.USER, domain.messages.first().role)
        assertEquals(2, domain.messages.first().content.size)
        assertEquals("weather", domain.tools.first().name)
        assertNotNull(domain.toolChoice)
        assertEquals(true, domain.providerOptions["stream"])
    }

    @Test
    fun `maps common response to anthropic message`() {
        val response = CommonResponse(
            provider = Provider.ANTHROPIC,
            id = "msg_123",
            model = "claude-3-5-sonnet",
            choices = listOf(
                CommonChoice(
                    index = 0,
                    message = CommonResponseMessage(
                        role = CommonRole.ASSISTANT,
                        content = listOf(
                            TextPart("Done"),
                            JsonPart(JsonValue.JsonObject(mapOf("status" to JsonValue.JsonString("ok")))),
                            ToolCallPart(
                                toolCallId = "toolu_1",
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

        val anthropic = AnthropicMessageResponse.fromDomain(response)

        assertEquals("msg_123", anthropic.id)
        assertEquals("assistant", anthropic.role)
        assertEquals("tool_use", anthropic.stopReason)
        assertEquals(2, anthropic.content.size)
        assertTrue(anthropic.content.first() is AnthropicOutputContent.Text)
        assertTrue(anthropic.content[1] is AnthropicOutputContent.ToolUse)
        assertEquals(10, anthropic.usage?.inputTokens)
        assertEquals(20, anthropic.usage?.outputTokens)
    }
}

