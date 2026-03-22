package org.omniaigateway.adapters.anthropic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.omniaigateway.contracts.anthropic.output.AnthropicMessageResponse
import org.omniaigateway.contracts.anthropic.output.AnthropicOutputContent
import org.omniaigateway.contracts.anthropic.output.AnthropicStreamEvent
import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.common.Model
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.requests.CommonRequest
import org.omniaigateway.domain.requests.CommonRequestMessage
import org.omniaigateway.domain.responses.ResponseStarted

class AnthropicAdapterTranslatorTest {

    private val translator = AnthropicAdapterTranslator()

    @Test
    fun `maps common request to anthropic request`() {
        val request = CommonRequest(
            provider = Provider.ANTHROPIC,
            model = "claude-3-5-sonnet",
            messages = listOf(
                CommonRequestMessage(
                    role = CommonRole.USER,
                    content = listOf(TextPart("Hello"))
                )
            ),
            systemPrompt = org.omniaigateway.domain.common.SystemPrompt("Be concise")
        )

        val anthropic = translator.fromDomain(request)

        assertEquals("claude-3-5-sonnet", anthropic.model)
        assertEquals("Be concise", anthropic.system)
        assertEquals(1, anthropic.messages.size)
        assertEquals("user", anthropic.messages.first().role)
    }

    @Test
    fun `maps anthropic response to common response`() {
        val response = AnthropicMessageResponse(
            id = "msg_123",
            type = "message",
            role = "assistant",
            model = "claude-3-5-sonnet",
            content = listOf(AnthropicOutputContent.Text("Done")),
            stopReason = "end_turn"
        )

        val domain = translator.toDomain(response)

        assertEquals(Provider.ANTHROPIC, domain.provider)
        assertEquals("msg_123", domain.id)
        assertEquals("Done", (domain.choices.first().message.content.first() as TextPart).text)
    }

    @Test
    fun `maps anthropic stream event to domain event`() {
        val event = AnthropicStreamEvent.MessageStart(
            message = AnthropicMessageResponse(
                id = "msg_1",
                type = "message",
                role = "assistant",
                model = "claude-3-5-sonnet"
            )
        )

        val domainEvent = translator.toDomainEvent(event)

        val started = assertIs<ResponseStarted>(domainEvent)
        assertEquals(Provider.ANTHROPIC, started.provider)
        assertEquals(Model("claude-3-5-sonnet"), started.model)
    }
}


