package org.omniaigateway.adapters.gemini

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.omniaigateway.contracts.gemini.output.GeminiCandidate
import org.omniaigateway.contracts.gemini.output.GeminiGenerateContentResponse
import org.omniaigateway.contracts.gemini.output.GeminiResponseContent
import org.omniaigateway.contracts.gemini.output.GeminiResponsePart
import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.common.Model
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.requests.CommonRequest
import org.omniaigateway.domain.requests.CommonRequestMessage
import org.omniaigateway.domain.responses.TextDeltaEvent

class GeminiAdapterTranslatorTest {

    private val translator = GeminiOutboundTranslator()

    @Test
    fun `maps common request to gemini request`() {
        val request = CommonRequest(
            provider = Provider.GEMINI,
            model = "gemini-2.0-flash",
            messages = listOf(
                CommonRequestMessage(
                    role = CommonRole.USER,
                    content = listOf(TextPart("Hello"))
                )
            ),
            systemPrompt = org.omniaigateway.domain.common.SystemPrompt("Keep it short")
        )

        val gemini = translator.fromDomain(request)

        assertEquals("gemini-2.0-flash", gemini.model)
        assertEquals("user", gemini.contents.first().role)
        assertEquals("Hello", gemini.contents.first().parts.first().text)
    }

    @Test
    fun `maps gemini response to common response`() {
        val response = GeminiGenerateContentResponse(
            candidates = listOf(
                GeminiCandidate(
                    index = 0,
                    content = GeminiResponseContent(
                        role = "model",
                        parts = listOf(GeminiResponsePart(text = "Done"))
                    ),
                    finishReason = "STOP"
                )
            ),
            modelVersion = "gemini-2.0-flash",
            responseId = "resp_123"
        )

        val domain = translator.toDomain(response)

        assertEquals(Provider.GEMINI, domain.provider)
        assertEquals("resp_123", domain.id)
        assertEquals("Done", (domain.choices.first().message.content.first() as TextPart).text)
    }

    @Test
    fun `maps gemini event to domain event`() {
        val event = GeminiGenerateContentResponse(
            candidates = listOf(
                GeminiCandidate(
                    index = 0,
                    content = GeminiResponseContent(parts = listOf(GeminiResponsePart(text = "partial")))
                )
            ),
            modelVersion = "gemini-2.0-flash",
            responseId = "resp_1"
        )

        val domainEvent = translator.toDomainEvent(event)

        val textDelta = assertIs<TextDeltaEvent>(domainEvent)
        assertEquals(Provider.GEMINI, textDelta.provider)
        assertEquals(Model("gemini-2.0-flash"), textDelta.model)
    }
}


