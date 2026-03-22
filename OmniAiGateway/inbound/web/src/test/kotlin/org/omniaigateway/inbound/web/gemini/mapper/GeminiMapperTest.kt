package org.omniaigateway.inbound.web.gemini.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
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
import org.omniaigateway.inbound.web.gemini.dto.input.GeminiContent
import org.omniaigateway.inbound.web.gemini.dto.input.GeminiFunctionCall
import org.omniaigateway.inbound.web.gemini.dto.input.GeminiFunctionCallingConfig
import org.omniaigateway.inbound.web.gemini.dto.input.GeminiFunctionDeclaration
import org.omniaigateway.inbound.web.gemini.dto.input.GeminiGenerateContentRequest
import org.omniaigateway.inbound.web.gemini.dto.input.GeminiGenerationConfig
import org.omniaigateway.inbound.web.gemini.dto.input.GeminiPart
import org.omniaigateway.inbound.web.gemini.dto.input.GeminiTool
import org.omniaigateway.inbound.web.gemini.dto.input.GeminiToolConfig
import org.omniaigateway.inbound.web.gemini.dto.output.GeminiGenerateContentResponse

class GeminiMapperTest {

    @Test
    fun `maps gemini request to common request`() {
        val request = GeminiGenerateContentRequest(
            model = "gemini-2.0-flash",
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(
                        GeminiPart(text = "hello"),
                        GeminiPart(
                            functionCall = GeminiFunctionCall(
                                id = "call_1",
                                name = "weather",
                                args = mapOf("city" to "Lisbon")
                            )
                        )
                    )
                )
            ),
            tools = listOf(
                GeminiTool(
                    functionDeclarations = listOf(
                        GeminiFunctionDeclaration(
                            name = "weather",
                            description = "Get weather",
                            parameters = mapOf("type" to "object")
                        )
                    )
                )
            ),
            toolConfig = GeminiToolConfig(
                functionCallingConfig = GeminiFunctionCallingConfig(
                    mode = "ANY",
                    allowedFunctionNames = listOf("weather")
                )
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.2,
                topP = 0.9,
                responseMimeType = "application/json"
            )
        )

        val domain = request.toDomain()

        assertEquals(Provider.GEMINI, domain.provider)
        assertEquals("gemini-2.0-flash", domain.model)
        assertEquals(1, domain.messages.size)
        assertEquals(CommonRole.USER, domain.messages.first().role)
        assertEquals(2, domain.messages.first().content.size)
        assertEquals("weather", domain.tools.first().name)
        assertNotNull(domain.toolChoice)
        assertTrue(domain.jsonResponse)
    }

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

        val gemini = GeminiGenerateContentResponse.fromDomain(response)

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

