package org.omniai.sdk.contracts.anthropic

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.omniai.sdk.contracts.anthropic.input.AnthropicInputContentBlock
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessageInput
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.input.AnthropicRole
import org.omniai.sdk.contracts.anthropic.input.AnthropicThinkingConfig
import org.omniai.sdk.contracts.anthropic.input.AnthropicToolChoice
import org.omniai.sdk.contracts.anthropic.input.AnthropicToolDefinition
import org.omniai.sdk.contracts.anthropic.input.ListContentBlock
import org.omniai.sdk.contracts.anthropic.input.RawText
import org.omniai.sdk.contracts.anthropic.output.AnthropicErrorResponse
import org.omniai.sdk.contracts.anthropic.output.AnthropicMessageResponse
import org.omniai.sdk.contracts.anthropic.output.AnthropicOutputContent
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamDelta
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent
import org.omniai.sdk.testutils.LocalHttpMockServer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AnthropicOfficialSdkContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = false
            encodeDefaults = true
        }

    @Test
    fun `simple compare sent text with received text`() {
        val sentText = "ping from sdk"
        val responseJson = anthropicResponseJson(sentText)

        LocalHttpMockServer(responseBody = responseJson).use { testServer ->
            testServer.start()

            val client: AnthropicClient =
                AnthropicOkHttpClient
                    .builder()
                    .apiKey("test-key")
                    .baseUrl(testServer.baseUrl)
                    .build()

            val params =
                MessageCreateParams
                    .builder()
                    .maxTokens(64L)
                    .model(Model.CLAUDE_OPUS_4_6)
                    .addUserMessage(sentText)
                    .build()

            client.messages().create(params)

            val requestJson = assertNotNull(testServer.capturedRequestBody)
            val requestDto = json.decodeFromString<AnthropicMessagesRequest>(requestJson)
            val requestContent = assertIs<RawText>(requestDto.messages.first().content)

            val responseDto = json.decodeFromString<AnthropicMessageResponse>(responseJson)
            val responseContent = responseDto.content.first() as AnthropicOutputContent.Text

            assertEquals(sentText, requestContent.text)
            assertEquals(requestContent.text, responseContent.text)
            assertEquals(sentText, responseContent.text)
        }
    }

    @Test
    fun `sdk request includes expected transport metadata and body fields`() {
        LocalHttpMockServer(responseBody = successResponseJson()).use { server ->
            server.start()

            val client: AnthropicClient =
                AnthropicOkHttpClient
                    .builder()
                    .apiKey("test-key")
                    .baseUrl(server.baseUrl)
                    .build()

            val params =
                MessageCreateParams
                    .builder()
                    .maxTokens(64L)
                    .model(Model.CLAUDE_OPUS_4_6)
                    .addUserMessage("hello from sdk")
                    .build()

            client.messages().create(params)

            val captured = assertNotNull(server.capturedRequest)
            val dto = json.decodeFromString<AnthropicMessagesRequest>(captured.body)

            assertEquals("POST", captured.method)
            assertEquals("/v1/messages", captured.path)
            assertEquals(captured.headers.getFirst("content-type")?.contains("application/json"), true)

            assertEquals("claude-opus-4-6", dto.model)
            assertEquals(64, dto.maxTokens)
            assertEquals(1, dto.messages.size)
            assertEquals(AnthropicRole.USER, dto.messages.first().role)
            assertEquals("hello from sdk", assertIs<RawText>(dto.messages.first().content).text)
        }
    }

    @Test
    fun `sdk preserves order when sending multiple user messages`() {
        LocalHttpMockServer(responseBody = successResponseJson()).use { server ->
            server.start()

            val client: AnthropicClient =
                AnthropicOkHttpClient
                    .builder()
                    .apiKey("test-key")
                    .baseUrl(server.baseUrl)
                    .build()

            val params =
                MessageCreateParams
                    .builder()
                    .maxTokens(32L)
                    .model(Model.CLAUDE_OPUS_4_6)
                    .addUserMessage("first")
                    .addUserMessage("second")
                    .addUserMessage("third")
                    .build()

            client.messages().create(params)

            val requestJson = assertNotNull(server.capturedRequestBody)
            val dto = json.decodeFromString<AnthropicMessagesRequest>(requestJson)

            assertEquals(3, dto.messages.size)
            assertEquals("first", assertIs<RawText>(dto.messages[0].content).text)
            assertEquals("second", assertIs<RawText>(dto.messages[1].content).text)
            assertEquals("third", assertIs<RawText>(dto.messages[2].content).text)
            assertTrue(dto.messages.all { it.role == AnthropicRole.USER })
        }
    }

    @Test
    fun `request DTO round-trips with all optional fields`() {
        val request =
            AnthropicMessagesRequest(
                model = "claude-opus-4-6",
                maxTokens = 256,
                messages =
                    listOf(
                        AnthropicMessageInput(
                            role = AnthropicRole.USER,
                            content =
                                ListContentBlock(
                                    blocks =
                                        listOf(
                                            AnthropicInputContentBlock.Text("hello"),
                                            AnthropicInputContentBlock.ToolUse(
                                                id = "toolu_1",
                                                name = "weather",
                                                input = json.parseToJsonElement("""{"city":"Porto"}""").jsonObject,
                                            ),
                                            AnthropicInputContentBlock.ToolResult(
                                                toolUseId = "toolu_1",
                                                content = "sunny",
                                            ),
                                            AnthropicInputContentBlock.Thinking(thinking = "Reasoning", signature = "sig_1"),
                                        ),
                                ),
                        ),
                    ),
                system = RawText("Be concise"),
                tools =
                    listOf(
                        AnthropicToolDefinition(
                            name = "weather",
                            description = "Get weather",
                            inputSchema =
                                JsonObject(
                                    mapOf(
                                        "type" to JsonPrimitive("object"),
                                        "properties" to
                                            JsonObject(
                                                mapOf(
                                                    "city" to
                                                        JsonObject(
                                                            mapOf("type" to JsonPrimitive("string")),
                                                        ),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
                toolChoice = AnthropicToolChoice(type = "tool", name = "weather"),
                stream = true,
                temperature = 0.2,
                topP = 0.9,
                topK = 32,
                stopSequences = listOf("STOP"),
                thinking = AnthropicThinkingConfig(type = "enabled", budgetTokens = 2048),
                metadata =
                    json.parseToJsonElement(
                        """{"traceId":"abc-123","attempt":1,"ok":true,"tags":["a","b"]}""",
                    ),
            )

        val encoded = json.encodeToString(request)
        val root = json.parseToJsonElement(encoded).jsonObject

        assertTrue("max_tokens" in root)
        assertTrue("tool_choice" in root)

        assertTrue("maxTokens" !in root)

        val decoded = json.decodeFromString<AnthropicMessagesRequest>(encoded)
        assertEquals(request, decoded)
    }

    @Test
    fun `response DTO parses text thinking and tool_use content`() {
        val payload =
            """
            {
              "id": "msg_42",
              "type": "message",
              "role": "assistant",
              "model": "claude-opus-4-6",
              "content": [
                {"type": "text", "text": "Done"},
                {"type": "thinking", "thinking": "Reasoning", "signature": "sig_1"},
                {"type": "tool_use", "id": "toolu_9", "name": "weather", "input": {"city": "Lisbon", "days": 2}}
              ],
              "stop_reason": "tool_use",
              "stop_sequence": null,
              "usage": {
                "input_tokens": 12,
                "output_tokens": 7,
                "cache_creation_input_tokens": 2,
                "cache_read_input_tokens": 1,
                "server_tool_use": {
                  "web_search_requests": 3
                }
              }
            }
            """.trimIndent()

        val response = json.decodeFromString<AnthropicMessageResponse>(payload)

        assertEquals("msg_42", response.id)
        assertEquals(org.omniai.sdk.contracts.anthropic.output.AnthropicStopReason.TOOL_USE, response.stopReason)

        val text = assertIs<AnthropicOutputContent.Text>(response.content[0])
        assertEquals("Done", text.text)

        val thinking = assertIs<AnthropicOutputContent.Thinking>(response.content[1])
        assertEquals("Reasoning", thinking.thinking)
        assertEquals("sig_1", thinking.signature)

        val toolUse = assertIs<AnthropicOutputContent.ToolUse>(response.content[2])
        assertEquals("weather", toolUse.name)
        assertEquals(
            "Lisbon",
            toolUse.input
                ?.get("city")
                ?.jsonPrimitive
                ?.content,
        )
        assertEquals(
            2L,
            toolUse.input
                ?.get("days")
                ?.jsonPrimitive
                ?.content
                ?.toLong(),
        )

        assertEquals(12, response.usage?.inputTokens)
        assertEquals(7, response.usage?.outputTokens)
        assertEquals(3, response.usage?.serverToolUse?.webSearchRequests)
    }

    @Test
    fun `stream event DTO parses all event variants`() {
        val variants =
            listOf(
                """
                {
                  "type": "message_start",
                  "message": {
                    "id": "msg_1",
                    "type": "message",
                    "role": "assistant",
                    "model": "claude-opus-4-6",
                    "content": []
                  }
                }
                """.trimIndent(),
                """
                {
                  "type": "content_block_start",
                  "index": 0,
                  "content_block": {"type": "text", "text": "hello"}
                }
                """.trimIndent(),
                """
                {
                  "type": "content_block_delta",
                  "index": 0,
                  "delta": {"type": "text_delta", "text": "hel"}
                }
                """.trimIndent(),
                """
                {
                  "type": "content_block_stop",
                  "index": 0
                }
                """.trimIndent(),
                """
                {
                  "type": "message_delta",
                  "delta": {"stop_reason": "end_turn", "stop_sequence": null},
                  "usage": {"input_tokens": 2, "output_tokens": 3}
                }
                """.trimIndent(),
                """{"type": "message_stop"}""",
                """{"type": "ping"}""",
                """
                {
                  "type": "error",
                  "error": {"type": "invalid_request_error", "message": "bad input"}
                }
                """.trimIndent(),
            )

        val parsed = variants.map { json.decodeFromString<AnthropicStreamEvent>(it) }

        assertIs<AnthropicStreamEvent.MessageStart>(parsed[0])
        assertIs<AnthropicStreamEvent.ContentBlockStart>(parsed[1])
        assertIs<AnthropicStreamEvent.ContentBlockDelta>(parsed[2])
        assertIs<AnthropicStreamEvent.ContentBlockStop>(parsed[3])
        assertIs<AnthropicStreamEvent.MessageDelta>(parsed[4])
        assertIs<AnthropicStreamEvent.MessageStop>(parsed[5])
        assertIs<AnthropicStreamEvent.Ping>(parsed[6])
        assertIs<AnthropicStreamEvent.Error>(parsed[7])
    }

    @Test
    fun `stream delta DTO parses all delta variants`() {
        val text = json.decodeFromString<AnthropicStreamDelta>("""{"type":"text_delta","text":"abc"}""")
        val thinking = json.decodeFromString<AnthropicStreamDelta>("""{"type":"thinking_delta","thinking":"reason"}""")
        val signature =
            json.decodeFromString<AnthropicStreamDelta>("""{"type":"signature_delta","signature":"sig_42"}""")
        val inputJson =
            json.decodeFromString<AnthropicStreamDelta>("""{"type":"input_json_delta","partial_json":"{\"k\":1}"}""")

        assertEquals("abc", assertIs<AnthropicStreamDelta.TextDelta>(text).text)
        assertEquals("reason", assertIs<AnthropicStreamDelta.ThinkingDelta>(thinking).thinking)
        assertEquals("sig_42", assertIs<AnthropicStreamDelta.SignatureDelta>(signature).signature)
        assertEquals("{\"k\":1}", assertIs<AnthropicStreamDelta.InputJsonDelta>(inputJson).partialJson)
    }

    @Test
    fun `error DTO parses canonical Anthropic error envelope`() {
        val payload =
            """
            {
              "type": "error",
              "error": {
                "type": "authentication_error",
                "message": "Invalid API key"
              }
            }
            """.trimIndent()

        val error = json.decodeFromString<AnthropicErrorResponse>(payload)

        assertEquals("error", error.type)
        assertEquals("authentication_error", error.error.type)
        assertEquals("Invalid API key", error.error.message)
    }

    @Test
    fun `content serializer accepts string and list forms`() {
        val rawPayload =
            """
            {
              "model": "claude-opus-4-6",
              "max_tokens": 8,
              "messages": [
                {"role": "user", "content": "plain text"}
              ]
            }
            """.trimIndent()

        val listPayload =
            """
            {
              "model": "claude-opus-4-6",
              "max_tokens": 8,
              "messages": [
                {
                  "role": "user",
                  "content": [
                    {"type": "text", "text": "part 1"},
                    {"type": "text", "text": "part 2"}
                  ]
                }
              ]
            }
            """.trimIndent()

        val rawRequest = json.decodeFromString<AnthropicMessagesRequest>(rawPayload)
        val listRequest = json.decodeFromString<AnthropicMessagesRequest>(listPayload)

        assertEquals("plain text", assertIs<RawText>(rawRequest.messages.first().content).text)
        val listContent = assertIs<ListContentBlock>(listRequest.messages.first().content)
        assertEquals(2, listContent.blocks.size)
    }

    @Test
    fun `serializers fail for unknown polymorphic types`() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<AnthropicOutputContent>("""{"type":"unknown_content"}""")
        }

        assertFailsWith<SerializationException> {
            json.decodeFromString<AnthropicStreamDelta>("""{"type":"unknown_delta"}""")
        }

        assertFailsWith<SerializationException> {
            json.decodeFromString<AnthropicStreamEvent>("""{"type":"unknown_event"}""")
        }
    }

    private fun anthropicResponseJson(text: String): String =
        """
        {
          "id": "msg_test_123",
          "type": "message",
          "role": "assistant",
          "model": "claude-opus-4-6",
          "content": [
            {
              "type": "text",
              "text": "$text"
            }
          ],
          "stop_reason": "end_turn",
          "usage": {
            "input_tokens": 12,
            "output_tokens": 5
          }
        }
        """.trimIndent()

    private fun successResponseJson(): String =
        """
        {
          "id": "msg_test_123",
          "type": "message",
          "role": "assistant",
          "model": "claude-opus-4-6",
          "content": [
            {
              "type": "text",
              "text": "ok"
            }
          ],
          "stop_reason": "end_turn",
          "usage": {
            "input_tokens": 1,
            "output_tokens": 1
          }
        }
        """.trimIndent()
}
