package org.omniai.sdk.contracts.openai

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.ChatModel
import com.openai.models.chat.completions.ChatCompletionCreateParams
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.omniai.sdk.contracts.openai.input.FunctionRef
import org.omniai.sdk.contracts.openai.input.OpenAiChatCompletionsRequest
import org.omniai.sdk.contracts.openai.input.OpenAiFunctionDefinition
import org.omniai.sdk.contracts.openai.input.OpenAiJsonSchema
import org.omniai.sdk.contracts.openai.input.OpenAiMessageInput
import org.omniai.sdk.contracts.openai.input.OpenAiResponseFormat
import org.omniai.sdk.contracts.openai.input.OpenAiStop
import org.omniai.sdk.contracts.openai.input.OpenAiTool
import org.omniai.sdk.contracts.openai.input.OpenAiToolCall
import org.omniai.sdk.contracts.openai.input.OpenAiToolCallFunction
import org.omniai.sdk.contracts.openai.input.OpenAiToolChoice
import org.omniai.sdk.contracts.openai.output.OpenAiChatCompletionsResponse
import org.omniai.sdk.contracts.openai.output.OpenAiDelta
import org.omniai.sdk.contracts.openai.output.OpenAiError
import org.omniai.sdk.contracts.openai.output.OpenAiMessageOutput

class OpenAiOfficialSdkContractTest {

    private val json = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
    }

    @Test
    fun `official SDK request maps to OpenAI request DTO`() {
        OpenAiLocalHttpTestServer(responseBody = completionResponseJson("pong")).use { server ->
            server.start()

            val client: OpenAIClient = OpenAIOkHttpClient.builder()
                .apiKey("test-key")
                .baseUrl(server.baseUrl)
                .build()

            val params = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_5_2)
                .addUserMessage("ping from sdk")
                .build()

            client.chat().completions().create(params)

            val captured = assertNotNull(server.capturedRequest)
            val requestDto = json.decodeFromString<OpenAiChatCompletionsRequest>(captured.body)
            val firstMessage = requestDto.messages.first()

            assertEquals("POST", captured.method)
            assertTrue(captured.path.endsWith("/chat/completions"))
            assertTrue(captured.headers.getFirst("content-type")?.contains("application/json") == true)

            assertEquals("gpt-5.2", requestDto.model)
            assertEquals("user", firstMessage.role)
            assertEquals("ping from sdk", firstMessage.content)
        }
    }

    @Test
    fun `official SDK keeps message order in DTO parsing`() {
        OpenAiLocalHttpTestServer(responseBody = completionResponseJson("ok")).use { server ->
            server.start()

            val client: OpenAIClient = OpenAIOkHttpClient.builder()
                .apiKey("test-key")
                .baseUrl(server.baseUrl)
                .build()

            val params = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_5_2)
                .addUserMessage("first")
                .addUserMessage("second")
                .addUserMessage("third")
                .build()

            client.chat().completions().create(params)

            val requestJson = assertNotNull(server.capturedRequestBody)
            val requestDto = json.decodeFromString<OpenAiChatCompletionsRequest>(requestJson)

            assertEquals(3, requestDto.messages.size)
            assertEquals("first", requestDto.messages[0].content)
            assertEquals("second", requestDto.messages[1].content)
            assertEquals("third", requestDto.messages[2].content)
            assertTrue(requestDto.messages.all { it.role == "user" })
        }
    }

    @Test
    fun `request DTO round-trips all input objects`() {
        val request = OpenAiChatCompletionsRequest(
            model = "gpt-5.2",
            messages = listOf(
                OpenAiMessageInput(role = "system", content = "Be concise"),
                OpenAiMessageInput(role = "user", content = "What is weather?"),
                OpenAiMessageInput(
                    role = "assistant",
                    toolCalls = listOf(
                        OpenAiToolCall(
                            id = "call_1",
                            index = 0,
                            function = OpenAiToolCallFunction(
                                name = "get_weather",
                                arguments = "{\"city\":\"Lisbon\"}"
                            )
                        )
                    )
                ),
                OpenAiMessageInput(role = "tool", toolCallId = "call_1", content = "sunny")
            ),
            temperature = 0.4,
            maxTokens = 120,
            topP = 0.8,
            stop = OpenAiStop.Multiple(listOf("STOP_A", "STOP_B")),
            frequencyPenalty = 0.1,
            presencePenalty = 0.2,
            n = 2,
            stream = false,
            seed = 42,
            user = "user-123",
            logitBias = mapOf("42" to 3),
            logProbs = true,
            topLogProbs = 2,
            responseFormat = OpenAiResponseFormat(
                type = "json_schema",
                jsonSchema = OpenAiJsonSchema(
                    name = "weather_reply",
                    strict = true,
                    schema = json.parseToJsonElement("""{"type":"object"}""").jsonObject
                )
            ),
            tools = listOf(
                OpenAiTool(
                    function = OpenAiFunctionDefinition(
                        name = "get_weather",
                        description = "Gets weather",
                        parameters = json.parseToJsonElement("""{"type":"object"}""").jsonObject
                    )
                )
            ),
            toolChoice = OpenAiToolChoice.Function(
                function = FunctionRef(name = "get_weather")
            )
        )

        val encoded = json.encodeToString(request)
        val root = json.parseToJsonElement(encoded).jsonObject

        assertTrue("max_tokens" in root)
        assertTrue("response_format" in root)
        assertTrue("tool_choice" in root)

        val decoded = json.decodeFromString<OpenAiChatCompletionsRequest>(encoded)
        assertEquals(request, decoded)
    }

    @Test
    fun `stop and tool_choice serializers support all expected forms`() {
        val stopSingle = OpenAiChatCompletionsRequest(
            model = "gpt-5.2",
            messages = listOf(OpenAiMessageInput(role = "user", content = "hi")),
            stop = OpenAiStop.Single("DONE"),
            toolChoice = OpenAiToolChoice.Mode("auto")
        )

        val encoded = json.encodeToString(stopSingle)
        val decoded = json.decodeFromString<OpenAiChatCompletionsRequest>(encoded)

        assertEquals(OpenAiStop.Single("DONE"), decoded.stop)
        assertEquals(OpenAiToolChoice.Mode("auto"), decoded.toolChoice)
    }

    @Test
    fun `response DTO parses message and usage objects`() {
        val payload = completionResponseJson("Done")
        val response = json.decodeFromString<OpenAiChatCompletionsResponse>(payload)

        assertEquals("chatcmpl_123", response.id)
        assertEquals("chat.completion", response.obj)
        assertEquals("gpt-5.2", response.model)

        val message = assertIs<OpenAiMessageOutput>(response.choices.first().message)
        assertEquals("assistant", message.role)
        assertEquals("Done", message.content)

        assertEquals(20, response.usage?.promptTokens)
        assertEquals(5, response.usage?.completionTokens)
        assertEquals(25, response.usage?.totalTokens)
    }

    @Test
    fun `response DTO parses streaming delta shapes`() {
        val streamPayload =
            """
            {
              "id": "chatcmpl_chunk_1",
              "object": "chat.completion.chunk",
              "created": 1721075653,
              "model": "gpt-5.2",
              "choices": [
                {
                  "index": 0,
                  "delta": {
                    "role": "assistant",
                    "content": "hel"
                  },
                  "finish_reason": null
                }
              ]
            }
            """.trimIndent()

        val response = json.decodeFromString<OpenAiChatCompletionsResponse>(streamPayload)
        val delta = assertIs<OpenAiDelta>(response.choices.first().delta)

        assertEquals("assistant", delta.role)
        assertEquals("hel", delta.content)
    }

    @Test
    fun `error DTO parses standard error payload`() {
        val payload =
            """
            {
              "message": "Invalid request",
              "type": "invalid_request_error",
              "param": "messages",
              "code": "bad_request"
            }
            """.trimIndent()

        val error = json.decodeFromString<OpenAiError>(payload)

        assertEquals("Invalid request", error.message)
        assertEquals("invalid_request_error", error.type)
        assertEquals("messages", error.param)
        assertEquals("bad_request", error.code)
    }

    @Test
    fun `serializers fail for invalid stop and tool_choice shapes`() {
        val invalidStopPayload =
            """
            {
              "model": "gpt-5.2",
              "messages": [{"role":"user","content":"hi"}],
              "stop": 123
            }
            """.trimIndent()

        val invalidToolChoicePayload =
            """
            {
              "model": "gpt-5.2",
              "messages": [{"role":"user","content":"hi"}],
              "tool_choice": 123
            }
            """.trimIndent()

        assertFailsWith<SerializationException> {
            json.decodeFromString<OpenAiChatCompletionsRequest>(invalidStopPayload)
        }

        assertFailsWith<SerializationException> {
            json.decodeFromString<OpenAiChatCompletionsRequest>(invalidToolChoicePayload)
        }
    }

    private fun completionResponseJson(text: String): String =
        """
        {
          "id": "chatcmpl_123",
          "object": "chat.completion",
          "created": 1721075651,
          "model": "gpt-5.2",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "$text"
              },
              "finish_reason": "stop"
            }
          ],
          "usage": {
            "prompt_tokens": 20,
            "completion_tokens": 5,
            "total_tokens": 25
          }
        }
        """.trimIndent()
}

