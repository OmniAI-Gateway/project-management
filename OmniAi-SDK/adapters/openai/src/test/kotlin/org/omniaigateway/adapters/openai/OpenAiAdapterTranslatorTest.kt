package org.omniaigateway.adapters.openai

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

class OpenAiAdapterTranslatorTest {

    private val translator = OpenAiAdapterTranslator()

    @Test
    fun `maps common response to openai response`() {
        val response = CommonResponse(
            provider = Provider.OPENAI,
            id = "chatcmpl_123",
            model = "gpt-4o-mini",
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

        val openAi = translator.fromDomain(response)

        assertEquals("chatcmpl_123", openAi.id)
        assertEquals("chat.completion", openAi.`object`)
        assertEquals("tool_calls", openAi.choices.first().finishReason)
        assertEquals("assistant", openAi.choices.first().message?.role)
        assertEquals("Done", openAi.choices.first().message?.content)
        assertEquals(1, openAi.choices.first().message?.toolCalls?.size)
        assertEquals("Lisbon", openAi.choices.first().message?.toolCalls?.first()?.function?.arguments?.get("city"))
        assertEquals(10, openAi.usage?.promptTokens)
        assertEquals(20, openAi.usage?.completionTokens)
    }
}


