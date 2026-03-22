package org.omniaigateway.adapters.gemini

import kotlin.test.Test
import kotlin.test.assertEquals
import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.common.content.ToolCallPart
import org.omniaigateway.domain.common.json.JsonValue
import org.omniaigateway.domain.responses.CommonChoice
import org.omniaigateway.domain.responses.CommonResponse
import org.omniaigateway.domain.responses.CommonResponseMessage
import org.omniaigateway.domain.responses.CommonUsage
import org.omniaigateway.domain.responses.FinishReason

class GeminiAdapterTranslatorTest {

    private val translator = GeminiAdapterTranslator()

    @Test
    fun `maps common response to gemini response`() {
        val response = CommonResponse(
            provider = Provider.GEMINI,
            id = "resp_123",
            model = "gemini-2.0-flash",
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

        val gemini = translator.fromDomain(response)

        assertEquals("resp_123", gemini.responseId)
        assertEquals("gemini-2.0-flash", gemini.modelVersion)
        assertEquals("STOP", gemini.candidates.first().finishReason)
        assertEquals("model", gemini.candidates.first().content?.role)
        assertEquals("Done", gemini.candidates.first().content?.parts?.first()?.text)
        assertEquals(2, gemini.candidates.first().content?.parts?.size)
        assertEquals("Lisbon", gemini.candidates.first().content?.parts?.get(1)?.functionCall?.args?.get("city"))
        assertEquals(10, gemini.usageMetadata?.promptTokenCount)
        assertEquals(20, gemini.usageMetadata?.candidatesTokenCount)
    }
}


