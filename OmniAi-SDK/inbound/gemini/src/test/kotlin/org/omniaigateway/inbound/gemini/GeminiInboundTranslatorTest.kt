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
import org.omniaigateway.domain.common.Provider

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
}

