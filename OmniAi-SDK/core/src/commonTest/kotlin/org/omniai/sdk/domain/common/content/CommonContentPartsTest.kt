package org.omniai.sdk.domain.common.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.omniai.sdk.domain.common.json.JsonValue

class CommonContentPartsTest {
    @Test
    fun `shared content part can be used as request and response`() {
        val text = TextPart("hello")
        val json = JsonPart(JsonValue.JsonObject(mapOf("ok" to JsonValue.JsonBoolean(true))))

        val requestParts: List<RequestContentPart> = listOf(text, json)
        val responseParts: List<ResponseContentPart> = listOf(text, json)

        assertEquals(2, requestParts.size)
        assertEquals(2, responseParts.size)
    }

    @Test
    fun `tool call part is shared content`() {
        val part = ToolCallPart(
            toolCallId = "call-1",
            functionName = "search",
            argumentsJson = mapOf("query" to JsonValue.JsonString("weather"))
        )
        val shared: SharedContentPart = part

        assertTrue(shared is ToolCallPart)
    }

    @Test
    fun `request and response specific parts keep their own contracts`() {
        val toolResult: RequestContentPart = ToolResultPart(
            toolCallId = "call-2",
            content = listOf(JsonValue.JsonString("ok"))
        )
        val refusal: ResponseContentPart = RefusalPart(reason = "safety")

        assertTrue(toolResult is ToolResultPart)
        assertTrue(refusal is RefusalPart)
    }
}

