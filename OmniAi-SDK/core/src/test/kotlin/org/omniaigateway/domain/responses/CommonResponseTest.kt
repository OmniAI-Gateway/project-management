package org.omniaigateway.domain.responses

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.common.content.RefusalPart
import org.omniaigateway.domain.common.content.TextPart

class CommonResponseTest {
    @Test
    fun `builds response with usage and finish reason`() {
        val responseMessage = CommonResponseMessage(
            role = CommonRole.ASSISTANT,
            content = listOf(TextPart("done"), RefusalPart("none"))
        )

        val choice = CommonChoice(
            index = 0,
            message = responseMessage,
            finishReason = FinishReason.STOP
        )

        val response = CommonResponse(
            provider = Provider.GEMINI,
            id = "resp-1",
            model = "gemini-2.0-flash",
            choices = listOf(choice),
            usage = CommonUsage(inputTokens = 10, outputTokens = 5, totalTokens = 15)
        )

        assertEquals(Provider.GEMINI, response.provider)
        assertEquals("resp-1", response.id)
        assertEquals(FinishReason.STOP, response.choices.first().finishReason)
        assertEquals(15, response.usage?.totalTokens)
    }

    @Test
    fun `keeps defaults when optional fields are omitted`() {
        val response = CommonResponse(
            provider = Provider.ANTHROPIC,
            model = "claude-sonnet",
            choices = emptyList()
        )

        assertNull(response.id)
        assertNull(response.usage)
        assertTrue(response.providerOptions.isEmpty())
    }
}

