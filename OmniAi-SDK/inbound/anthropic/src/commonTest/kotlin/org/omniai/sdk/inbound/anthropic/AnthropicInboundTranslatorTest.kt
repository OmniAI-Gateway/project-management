package org.omniai.sdk.inbound.anthropic

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.omniai.sdk.contracts.anthropic.input.AnthropicInputContentBlock
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessageInput
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.input.AnthropicToolChoice
import org.omniai.sdk.contracts.anthropic.input.AnthropicToolDefinition
import org.omniai.sdk.contracts.anthropic.input.ListContentBlock
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.CommonRole
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.common.content.TextPart
import org.omniai.sdk.domain.responses.CommonChoice
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseMessage
import org.omniai.sdk.domain.responses.CommonUsage
import org.omniai.sdk.domain.responses.FinishReason
import org.omniai.sdk.domain.responses.ResponseStarted
import org.omniai.sdk.domain.responses.UsageReported

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
                                input = JsonObject(mapOf("city" to JsonPrimitive("Lisbon")))
                            )
                        )
                    )
                )
            ),
            tools = listOf(
                AnthropicToolDefinition(
                    name = "weather",
                    description = "Get weather",
                    inputSchema = JsonObject(mapOf("type" to JsonPrimitive("object")))
                )
            ),
            toolChoice = AnthropicToolChoice(type = "tool", name = "weather"),
            temperature = 0.3,
            stream = true,
            metadata = JsonObject(mapOf("traceId" to JsonPrimitive("abc-123")))
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

    @Test
    fun `maps common response to anthropic message`() {
        val domainResponse = CommonResponse(
            provider = Provider.ANTHROPIC,
            id = "msg_123",
            model = "claude-3-5-sonnet",
            choices = listOf(
                CommonChoice(
                    index = 0,
                    message = CommonResponseMessage(
                        role = CommonRole.ASSISTANT,
                        content = listOf(TextPart("Done"))
                    ),
                    finishReason = FinishReason.STOP
                )
            ),
            usage = CommonUsage(inputTokens = 12, outputTokens = 8)
        )

        val response = translator.fromDomain(domainResponse)

        assertEquals("msg_123", response.id)
        assertEquals("assistant", response.role)
        assertEquals("end_turn", response.stopReason)
        assertEquals(1, response.content.size)
        assertEquals(12, response.usage?.inputTokens)
    }

    @Test
    fun `maps common stream events to anthropic events`() = runTest {
        val started = translator.fromDomainEvent(
            flowOf(
                ResponseStarted(
                    provider = Provider.ANTHROPIC,
                    id = "msg_abc",
                    model = Model("claude-3-5-sonnet"),
                    sequence = 1
                )
            )
        ).first()
        assertTrue(started is AnthropicStreamEvent.MessageStart)
        assertEquals("msg_abc", started.message.id)
        assertEquals("msg_abc", started.message.id)

        val usage = translator.fromDomainEvent(
            flowOf(
                UsageReported(
                    provider = Provider.ANTHROPIC,
                    id = "msg_abc",
                    model = Model("claude-3-5-sonnet"),
                    sequence = 2,
                    usage = CommonUsage(inputTokens = 3, outputTokens = 4)
                )
            )
        ).first()
        assertTrue(usage is AnthropicStreamEvent.MessageDelta)
        assertEquals(3, usage.usage?.inputTokens)
        assertEquals(3, usage.usage?.inputTokens)
        assertEquals(4, usage.usage?.outputTokens)
    }
}
