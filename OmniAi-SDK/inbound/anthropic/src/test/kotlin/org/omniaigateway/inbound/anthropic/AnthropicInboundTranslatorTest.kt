package org.omniaigateway.inbound.anthropic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.omniaigateway.contracts.anthropic.input.AnthropicInputContentBlock
import org.omniaigateway.contracts.anthropic.input.AnthropicMessageInput
import org.omniaigateway.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniaigateway.contracts.anthropic.input.AnthropicToolChoice
import org.omniaigateway.contracts.anthropic.input.AnthropicToolDefinition
import org.omniaigateway.contracts.anthropic.input.ListContentBlock
import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.Provider

class AnthropicInboundTranslatorTest {

    private val translator = AnthropicInboundTranslator()

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

        val domain = translator.toDomain(request)

        assertEquals(Provider.ANTHROPIC, domain.provider)
        assertEquals("claude-3-5-sonnet", domain.model)
        assertEquals(1, domain.messages.size)
        assertEquals(CommonRole.USER, domain.messages.first().role)
        assertEquals(2, domain.messages.first().content.size)
        assertEquals("weather", domain.tools.first().name)
        assertNotNull(domain.toolChoice)
        assertEquals(true, domain.providerOptions["stream"])
    }
}

