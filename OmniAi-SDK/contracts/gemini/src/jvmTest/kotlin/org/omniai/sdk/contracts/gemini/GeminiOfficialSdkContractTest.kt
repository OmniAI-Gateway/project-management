package org.omniai.sdk.contracts.gemini

import com.google.genai.Client
import com.google.genai.types.HttpOptions
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertTrue
import org.omniai.sdk.contracts.gemini.input.GeminiContent
import org.omniai.sdk.contracts.gemini.input.GeminiFunctionCallingConfig
import org.omniai.sdk.contracts.gemini.input.GeminiFunctionDeclaration
import org.omniai.sdk.contracts.gemini.input.GeminiFunctionResponse
import org.omniai.sdk.contracts.gemini.input.GeminiGenerateContentRequest
import org.omniai.sdk.contracts.gemini.input.GeminiGenerationConfig
import org.omniai.sdk.contracts.gemini.input.GeminiInlineData
import org.omniai.sdk.contracts.gemini.input.GeminiPart
import org.omniai.sdk.contracts.gemini.input.GeminiSystemInstruction
import org.omniai.sdk.contracts.gemini.input.GeminiThinkingConfig
import org.omniai.sdk.contracts.gemini.input.GeminiTool
import org.omniai.sdk.contracts.gemini.input.GeminiToolConfig
import org.omniai.sdk.contracts.gemini.output.GeminiErrorResponse
import org.omniai.sdk.contracts.gemini.output.GeminiGenerateContentResponse
import org.omniai.sdk.contracts.gemini.output.GeminiResponsePart
import org.omniai.sdk.testutils.LocalHttpMockServer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import org.omniai.sdk.contracts.gemini.input.GeminiFunctionCall as GeminiInputFunctionCall
import org.omniai.sdk.contracts.gemini.output.GeminiFunctionCall as GeminiOutputFunctionCall

class GeminiOfficialSdkContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = false
            encodeDefaults = true
        }

    @Test
    fun `official SDK request maps to Gemini request DTO`() {
        LocalHttpMockServer(responseBody = sdkResponseJson("pong")).use { server ->
            server.start()

            val client =
                Client
                    .builder()
                    .apiKey("test-key")
                    .httpOptions(HttpOptions.builder().baseUrl(server.baseUrl).build())
                    .build()

            client.models.generateContent("gemini-2.5-flash", "ping from sdk", null)

            val captured = assertNotNull(server.capturedRequest)
            val requestDto = json.decodeFromString<GeminiGenerateContentRequest>(captured.body)
            val firstPart =
                requestDto.contents
                    .first()
                    .parts
                    .first()

            assertEquals("POST", captured.method)
            assertTrue(captured.path.contains(":generateContent"))
            assertTrue(captured.headers.getFirst("content-type")?.contains("application/json") == true)

            assertEquals("user", requestDto.contents.first().role)
            assertEquals("ping from sdk", firstPart.text)
        }
    }

    @Test
    fun `request DTO round-trips all input objects`() {
        val request =
            GeminiGenerateContentRequest(
                contents =
                    listOf(
                        GeminiContent(
                            role = "user",
                            parts =
                                listOf(
                                    GeminiPart(text = "What is weather?"),
                                    GeminiPart(
                                        inlineData =
                                            GeminiInlineData(
                                                mimeType = "text/plain",
                                                data = "SGVsbG8=",
                                            ),
                                    ),
                                    GeminiPart(
                                        functionCall =
                                            GeminiInputFunctionCall(
                                                name = "get_weather",
                                                args = json.parseToJsonElement("""{"city":"Lisbon","days":2}""").jsonObject,
                                            ),
                                    ),
                                    GeminiPart(
                                        functionResponse =
                                            GeminiFunctionResponse(
                                                name = "get_weather",
                                                response = json.parseToJsonElement("""{"forecast":"sunny","confidence":99}""").jsonObject,
                                            ),
                                    ),
                                ),
                        ),
                    ),
                systemInstruction =
                    GeminiSystemInstruction(
                        parts = listOf(GeminiPart(text = "Be concise")),
                    ),
                tools =
                    listOf(
                        GeminiTool(
                            functionDeclarations =
                                listOf(
                                    GeminiFunctionDeclaration(
                                        name = "get_weather",
                                        description = "Gets weather",
                                        parameters =
                                            json
                                                .parseToJsonElement(
                                                    """{"type":"object","properties":{"city":{"type":"string"}}}""",
                                                ).jsonObject,
                                    ),
                                ),
                            googleSearch = json.parseToJsonElement("""{"enabled":true}""").jsonObject,
                            urlContext = json.parseToJsonElement("""{"enabled":true}""").jsonObject,
                        ),
                    ),
                toolConfig =
                    GeminiToolConfig(
                        functionCallingConfig =
                            GeminiFunctionCallingConfig(
                                mode = "ANY",
                                allowedFunctionNames = listOf("get_weather"),
                            ),
                    ),
                generationConfig =
                    GeminiGenerationConfig(
                        stopSequences = listOf("STOP"),
                        temperature = 0.4,
                        topP = 0.9,
                        topK = 32,
                        thinkingConfig =
                            GeminiThinkingConfig(
                                includeThoughts = true,
                                includeThoughtSignature = true,
                                thinkingLevel = "HIGH",
                            ),
                        responseMimeType = "application/json",
                        responseJsonSchema = json.parseToJsonElement("""{"type":"object","strict":true}""").jsonObject,
                    ),
            )

        val encoded = json.encodeToString(request)
        val root = json.parseToJsonElement(encoded).jsonObject

        assertTrue("system_instruction" in root)
        assertTrue("generationConfig" in root)

        val decoded = json.decodeFromString<GeminiGenerateContentRequest>(encoded)
        assertEquals(request, decoded)
    }

    @Test
    fun `response DTO parses candidates usage and prompt feedback`() {
        val payload =
            """
            {
              "candidates": [
                {
                  "content": {
                    "role": "model",
                    "parts": [
                      {"text": "Done"},
                      {"functionCall": {"name": "get_weather", "args": {"city": "Lisbon"}}}
                    ]
                  },
                  "finishReason": "STOP",
                  "finishMessage": "complete",
                  "index": 0
                }
              ],
              "usageMetadata": {
                "promptTokenCount": 10,
                "candidatesTokenCount": 5,
                "totalTokenCount": 15,
                "thoughtsTokenCount": 2,
                "promptTokensDetails": [{"modality": "TEXT", "tokenCount": 10}],
                "candidatesTokensDetails": [{"modality": "TEXT", "tokenCount": 5}]
              },
              "modelVersion": "gemini-2.5-flash",
              "responseId": "resp_1",
              "promptFeedback": {
                "blockReason": "NONE",
                "safetyRatings": [
                  {"category": "HARM_CATEGORY_DANGEROUS_CONTENT", "probability": "NEGLIGIBLE", "blocked": false}
                ]
              }
            }
            """.trimIndent()

        val response = json.decodeFromString<GeminiGenerateContentResponse>(payload)

        assertEquals("gemini-2.5-flash", response.modelVersion)
        assertEquals("resp_1", response.responseId)
        assertEquals(15, response.usageMetadata?.totalTokenCount)
        assertEquals("NONE", response.promptFeedback?.blockReason)

        val firstPart =
            response.candidates
                .first()
                .content
                ?.parts
                ?.get(0)
        assertEquals("Done", assertIs<GeminiResponsePart>(firstPart).text)

        val functionCall =
            response.candidates
                .first()
                .content
                ?.parts
                ?.get(1)
                ?.functionCall
        assertEquals("get_weather", assertIs<GeminiOutputFunctionCall>(functionCall).name)
        assertEquals(
            "Lisbon",
            functionCall.args
                ?.get("city")
                ?.jsonPrimitive
                ?.content,
        )
    }

    @Test
    fun `dynamic map serializers preserve primitive and nested values`() {
        val payload =
            """
            {
              "contents": [
                {
                  "role": "user",
                  "parts": [
                    {
                      "functionCall": {
                        "name": "do_work",
                        "args": {
                          "ok": true,
                          "attempt": 3,
                          "ratio": 0.75,
                          "nested": {"k": "v"}
                        }
                      }
                    }
                  ]
                }
              ]
            }
            """.trimIndent()

        val request = json.decodeFromString<GeminiGenerateContentRequest>(payload)
        val args =
            request.contents
                .first()
                .parts
                .first()
                .functionCall
                ?.args ?: error("missing args")

        assertEquals("true", args["ok"]?.jsonPrimitive?.content)
        assertEquals(3L, args["attempt"]?.jsonPrimitive?.content?.toLong())
        assertEquals(0.75, args["ratio"]?.jsonPrimitive?.content?.toDouble())
        assertEquals(
            "v",
            args["nested"]
                ?.jsonObject
                ?.get("k")
                ?.jsonPrimitive
                ?.content,
        )
    }

    @Test
    fun `serializer fails when function response is not an object`() {
        val payload =
            """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "functionResponse": {
                        "name": "get_weather",
                        "response": ["invalid"]
                      }
                    }
                  ]
                }
              ]
            }
            """.trimIndent()

        assertFailsWith<SerializationException> {
            json.decodeFromString<GeminiGenerateContentRequest>(payload)
        }
    }

    @Test
    fun `error DTO parses canonical Gemini error envelope`() {
        val payload =
            """
            {
              "error": {
                "code": 429,
                "message": "Quota exceeded for requests per minute.",
                "status": "RESOURCE_EXHAUSTED",
                "details": [
                  {
                    "@type": "type.googleapis.com/google.rpc.ErrorInfo",
                    "reason": "RATE_LIMIT_EXCEEDED",
                    "domain": "googleapis.com",
                    "metadata": {
                      "service": "generativelanguage.googleapis.com"
                    }
                  }
                ]
              }
            }
            """.trimIndent()

        val error = json.decodeFromString<GeminiErrorResponse>(payload)

        assertEquals(429, error.error.code)
        assertEquals("RESOURCE_EXHAUSTED", error.error.status)
        assertEquals(
            "RATE_LIMIT_EXCEEDED",
            error.error.details
                .first()
                .reason,
        )
        assertEquals(
            "generativelanguage.googleapis.com",
            error.error.details
                .first()
                .metadata.service,
        )
    }

    private fun sdkResponseJson(text: String): String =
        """
        {
          "candidates": [
            {
              "content": {
                "role": "model",
                "parts": [
                  {"text": "$text"}
                ]
              },
              "finishReason": "STOP",
              "index": 0
            }
          ]
        }
        """.trimIndent()
}
