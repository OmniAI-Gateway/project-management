package org.omniaigateway.adapters.openai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.omniaigateway.contracts.openai.output.OpenAiChatCompletionsResponse
import org.omniaigateway.contracts.openai.output.OpenAiChoice
import org.omniaigateway.contracts.openai.output.OpenAiDelta
import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.common.Model
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.requests.CommonRequest
import org.omniaigateway.domain.requests.CommonRequestMessage
import org.omniaigateway.domain.responses.TextDeltaEvent

class OpenAiAdapterTranslatorTest {

    private val translator = OpenAiAdapterTranslator()

    @Test
    fun `maps common request to openai request`() {
        val request = CommonRequest(
            provider = Provider.OPENAI,
            model = "gpt-4o-mini",
            messages = listOf(
                CommonRequestMessage(
                    role = CommonRole.USER,
                    content = listOf(TextPart("Hello"))
                )
            ),
            config = org.omniaigateway.domain.common.CommonGenerationConfig(maxTokens = 120)
        )

        val openAi = translator.fromDomain(request)

        assertEquals("gpt-4o-mini", openAi.model)
        assertEquals("user", openAi.messages.first().role)
        assertEquals("Hello", openAi.messages.first().content)
        assertEquals(120, openAi.maxTokens)
    }

    @Test
    fun `maps openai response to common response`() {
        val response = OpenAiChatCompletionsResponse(
            id = "chatcmpl_123",
            `object` = "chat.completion",
            created = 1,
            model = "gpt-4o-mini",
            choices = listOf(
                OpenAiChoice(
                    index = 0,
                    message = org.omniaigateway.contracts.openai.output.OpenAiMessageOutput(
                        role = "assistant",
                        content = "Done"
                    ),
                    finishReason = "stop"
                )
            )
        )

        val domain = translator.toDomain(response)

        assertEquals(Provider.OPENAI, domain.provider)
        assertEquals("chatcmpl_123", domain.id)
        assertEquals("Done", (domain.choices.first().message.content.first() as TextPart).text)
    }

    @Test
    fun `maps openai chunk event to domain event`() {
        val event = OpenAiChatCompletionsResponse(
            id = "chatcmpl_1",
            `object` = "chat.completion.chunk",
            created = 2,
            model = "gpt-4o-mini",
            choices = listOf(
                OpenAiChoice(
                    index = 0,
                    delta = OpenAiDelta(content = "partial")
                )
            )
        )

        val domainEvent = translator.toDomainEvent(event)

        val textDelta = assertIs<TextDeltaEvent>(domainEvent)
        assertEquals(Provider.OPENAI, textDelta.provider)
        assertEquals(Model("gpt-4o-mini"), textDelta.model)
    }
}


