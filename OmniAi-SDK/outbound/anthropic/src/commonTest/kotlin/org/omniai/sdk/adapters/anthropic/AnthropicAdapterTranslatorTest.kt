package org.omniai.sdk.adapters.anthropic

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.omniai.sdk.contracts.anthropic.input.AnthropicRole
import org.omniai.sdk.contracts.anthropic.input.RawText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.omniai.sdk.contracts.anthropic.output.AnthropicMessageResponse
import org.omniai.sdk.contracts.anthropic.output.AnthropicOutputContent
import org.omniai.sdk.contracts.anthropic.output.AnthropicStopReason
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent
import org.omniai.sdk.domain.common.CommonRole
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.SystemPrompt
import org.omniai.sdk.domain.common.content.TextPart
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.requests.CommonRequestMessage
import org.omniai.sdk.domain.responses.ResponseStarted

class AnthropicAdapterTranslatorTest {

    private val translator = AnthropicOutboundTranslator()

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
            systemPrompt = SystemPrompt("Be concise")
        )

        val anthropic = translator.fromDomain(request)
        assertEquals("claude-3-5-sonnet", anthropic.model)
        assertEquals(RawText("Be concise"), anthropic.system)
        assertEquals(1, anthropic.messages.size)
        assertEquals(AnthropicRole.USER, anthropic.messages.first().role)
    }

    @Test
    fun `maps anthropic response to common response`() {
        val response = AnthropicMessageResponse(
            id = "msg_123",
            type = "message",
            role = "assistant",
            model = "claude-3-5-sonnet",
            content = listOf(AnthropicOutputContent.Text("Done")),
            stopReason = AnthropicStopReason.END_TURN
        )

        val domain = translator.toDomain(response)

        assertEquals(Provider.ANTHROPIC, domain.provider)
        assertEquals("msg_123", domain.id)
        assertEquals("Done", (domain.choices.first().message.content.first() as TextPart).text)
    }

    @Test
    fun `maps anthropic stream event to domain event`() = runTest {
        val event = AnthropicStreamEvent.MessageStart(
            message = AnthropicMessageResponse(
                id = "msg_1",
                type = "message",
                role = "assistant",
                model = "claude-3-5-sonnet"
            )
        )

        val domainEvent = translator.toDomainEvent(flowOf(event)).first()

        val started = assertIs<ResponseStarted>(domainEvent)
        assertEquals(Provider.ANTHROPIC, started.provider)
        assertEquals(Model("claude-3-5-sonnet"), started.model)
    }
}


