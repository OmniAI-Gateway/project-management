package org.omniaigateway.domain.requests

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.common.ToolChoice
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.common.content.ToolResultPart
import org.omniaigateway.domain.common.json.JsonValue

class CommonRequestTest {
    @Test
    fun `builds request with defaults`() {
        val message = CommonRequestMessage(
            role = CommonRole.USER,
            content = listOf(TextPart("hello"))
        )

        val request = CommonRequest(
            provider = Provider.OPENAI,
            model = "gpt-4o-mini",
            messages = listOf(message)
        )

        assertEquals(Provider.OPENAI, request.provider)
        assertEquals("gpt-4o-mini", request.model)
        assertEquals(1, request.messages.size)
        assertTrue(request.tools.isEmpty())
        assertFalse(request.jsonResponse)
        assertTrue(request.providerOptions.isEmpty())
    }

    @Test
    fun `accepts shared and request only content parts`() {
        val message = CommonRequestMessage(
            role = CommonRole.TOOL,
            content = listOf(
                TextPart("tool output"),
                ToolResultPart(
                    toolCallId = "call-1",
                    content = listOf(JsonValue.JsonObject(mapOf("ok" to JsonValue.JsonBoolean(true))))
                )
            )
        )

        assertEquals(CommonRole.TOOL, message.role)
        assertEquals(2, message.content.size)
    }

    @Test
    fun `supports tool choice specific convenience constructor`() {
        val specific = ToolChoice.Specific("search")

        assertEquals(listOf("search"), specific.toolNames)
    }
}

