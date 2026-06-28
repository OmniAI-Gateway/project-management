package org.omniai.sdk.adapters.gemini

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.omniai.sdk.contracts.gemini.output.GeminiCandidate
import org.omniai.sdk.contracts.gemini.output.GeminiEventStream
import org.omniai.sdk.contracts.gemini.output.GeminiGenerateContentResponse
import org.omniai.sdk.contracts.gemini.output.GeminiResponseContent
import org.omniai.sdk.contracts.gemini.output.GeminiResponsePart
import org.omniai.sdk.domain.common.CommonRole
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.common.content.TextPart
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.requests.CommonRequestMessage
import org.omniai.sdk.domain.responses.TextDeltaEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GeminiAdapterTranslatorTest {
    private val translator = GeminiOutboundTranslator()

    @Test
    fun `maps common request to gemini request`() {
        val request =
            CommonRequest(
                provider = Provider.GEMINI,
                model = "gemini-2.0-flash",
                messages =
                    listOf(
                        CommonRequestMessage(
                            role = CommonRole.USER,
                            content = listOf(TextPart("Hello")),
                        ),
                    ),
                systemPrompt =
                    org.omniai.sdk.domain.common
                        .SystemPrompt("Keep it short"),
            )

        val gemini = translator.fromDomain(request)

        assertEquals("user", gemini.contents.first().role)
        assertEquals(
            "Hello",
            gemini.contents
                .first()
                .parts
                .first()
                .text,
        )
    }

    @Test
    fun `maps gemini event to domain event`() =
        runTest {
            val event =
                GeminiGenerateContentResponse(
                    candidates =
                        listOf(
                            GeminiCandidate(
                                index = 0,
                                content = GeminiResponseContent(parts = listOf(GeminiResponsePart(text = "partial"))),
                            ),
                        ),
                    modelVersion = "gemini-2.0-flash",
                    responseId = "resp_1",
                )

            val domainEvent =
                translator
                    .toDomainEvent(flowOf(GeminiEventStream.Chunk(event)))
                    .first()

            val textDelta = assertIs<TextDeltaEvent>(domainEvent)
            assertEquals(Provider.GEMINI, textDelta.provider)
        }
}
