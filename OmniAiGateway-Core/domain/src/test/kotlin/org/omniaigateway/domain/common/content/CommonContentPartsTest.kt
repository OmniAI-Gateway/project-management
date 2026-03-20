package org.omniaigateway.domain.common.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommonContentPartsTest {
    @Test
    fun `shared content part can be used as request and response`() {
        val text = TextPart("hello")

        val requestParts: List<RequestContentPart> = listOf(text)
        val responseParts: List<ResponseContentPart> = listOf(text)

        assertEquals(1, requestParts.size)
        assertEquals(1, responseParts.size)
    }

    @Test
    fun `tool call part is shared content`() {
        val part = ToolCallPart(
            toolCallId = "call-1",
            functionName = "search",
            argumentsJson = mapOf("query" to "weather")
        )
        val shared: SharedContentPart = part

        assertTrue(shared is ToolCallPart)
    }

    @Test
    fun `request and response specific parts keep their own contracts`() {
        val toolResult: RequestContentPart = ToolResultPart(
            toolCallId = "call-2",
            content = listOf("ok")
        )
        val refusal: ResponseContentPart = RefusalPart(reason = "safety")

        assertTrue(toolResult is ToolResultPart)
        assertTrue(refusal is RefusalPart)
    }
}

