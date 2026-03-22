package org.omniaigateway.adapters.anthropic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.omniaigateway.contracts.anthropic.output.AnthropicOutputContent
import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.common.content.JsonPart
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.common.content.ToolCallPart
import org.omniaigateway.domain.common.json.JsonValue
import org.omniaigateway.domain.responses.CommonChoice
import org.omniaigateway.domain.responses.CommonResponse
import org.omniaigateway.domain.responses.CommonResponseMessage
import org.omniaigateway.domain.responses.CommonUsage
import org.omniaigateway.domain.responses.FinishReason

class AnthropicResponseTranslatorTest {

    private val translator = AnthropicResponseTranslator()

    @Test
    fun `maps common response to anthropic message`() {
        val response = CommonResponse(
            provider = Provider.ANTHROPIC,
            id = "msg_123",
            model = "claude-3-5-sonnet",
            choices = listOf(
                CommonChoice(
                    index = 0,
                    message = CommonResponseMessage(
                        role = CommonRole.ASSISTANT,
                        content = listOf(
                            TextPart("Done"),
                            JsonPart(JsonValue.JsonObject(mapOf("status" to JsonValue.JsonString("ok")))),
                            ToolCallPart(
                                toolCallId = "toolu_1",
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

        val anthropic = translator.fromDomain(response)

        assertEquals("msg_123", anthropic.id)
        assertEquals("assistant", anthropic.role)
        assertEquals("tool_use", anthropic.stopReason)
        assertEquals(2, anthropic.content.size)
        assertTrue(anthropic.content.first() is AnthropicOutputContent.Text)
        assertTrue(anthropic.content[1] is AnthropicOutputContent.ToolUse)
        assertEquals(10, anthropic.usage?.inputTokens)
        assertEquals(20, anthropic.usage?.outputTokens)
    }
}

