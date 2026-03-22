package org.omniaigateway.inbound.gemini

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.omniaigateway.contracts.gemini.input.GeminiContent
import org.omniaigateway.contracts.gemini.input.GeminiFunctionCall
import org.omniaigateway.contracts.gemini.input.GeminiFunctionCallingConfig
import org.omniaigateway.contracts.gemini.input.GeminiFunctionDeclaration
import org.omniaigateway.contracts.gemini.input.GeminiGenerateContentRequest
import org.omniaigateway.contracts.gemini.input.GeminiGenerationConfig
import org.omniaigateway.contracts.gemini.input.GeminiPart
import org.omniaigateway.contracts.gemini.input.GeminiTool
import org.omniaigateway.contracts.gemini.input.GeminiToolConfig
import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.Model
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.common.content.ToolCallPart
import org.omniaigateway.domain.common.json.JsonValue
import org.omniaigateway.domain.responses.CommonChoice
import org.omniaigateway.domain.responses.CommonResponse
import org.omniaigateway.domain.responses.CommonResponseMessage
import org.omniaigateway.domain.responses.CommonUsage
import org.omniaigateway.domain.responses.FinishReason
import org.omniaigateway.domain.responses.ResponseStarted
import org.omniaigateway.domain.responses.ToolCallStartedEvent
import org.omniaigateway.domain.responses.UsageReported

class GeminiInboundTranslatorTest {

    private val translator = GeminiInboundTranslator()

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

        val domain = translator.toDomain(request)

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
        val domainResponse = CommonResponse(
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

        val response = translator.fromDomain(domainResponse)

        assertEquals("resp_123", response.responseId)
        assertEquals("gemini-2.0-flash", response.modelVersion)
        assertEquals("STOP", response.candidates.first().finishReason)
        assertEquals("model", response.candidates.first().content?.role)
        assertEquals("Done", response.candidates.first().content?.parts?.first()?.text)
        assertEquals("Lisbon", response.candidates.first().content?.parts?.get(1)?.functionCall?.args?.get("city"))
        assertEquals(10, response.usageMetadata?.promptTokenCount)
    }

    @Test
    fun `maps common stream events to gemini response chunks`() {
        val started = translator.fromDomainEvent(
            ResponseStarted(
                provider = Provider.GEMINI,
                id = "resp_abc",
                model = Model("gemini-2.0-flash"),
                sequence = 1
            )
        )
        assertEquals("resp_abc", started.responseId)
        assertEquals("gemini-2.0-flash", started.modelVersion)

        val toolCallStarted = translator.fromDomainEvent(
            ToolCallStartedEvent(
                provider = Provider.GEMINI,
                id = "resp_abc",
                model = Model("gemini-2.0-flash"),
                sequence = 2,
                choiceIndex = 0,
                toolCallIndex = 0,
                toolCallId = "call_1",
                functionName = "weather"
            )
        )
        assertEquals("weather", toolCallStarted.candidates.first().content?.parts?.first()?.functionCall?.name)

        val usage = translator.fromDomainEvent(
            UsageReported(
                provider = Provider.GEMINI,
                id = "resp_abc",
                model = Model("gemini-2.0-flash"),
                sequence = 3,
                usage = CommonUsage(inputTokens = 3, outputTokens = 4, totalTokens = 7)
            )
        )
        assertEquals(3, usage.usageMetadata?.promptTokenCount)
        assertEquals(4, usage.usageMetadata?.candidatesTokenCount)
        assertEquals(7, usage.usageMetadata?.totalTokenCount)
    }
}

