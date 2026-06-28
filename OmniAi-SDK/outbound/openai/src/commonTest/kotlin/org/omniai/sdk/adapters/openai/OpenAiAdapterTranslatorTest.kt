package org.omniai.sdk.adapters.openai

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.omniai.sdk.contracts.openai.output.OpenAiChatCompletionsResponse
import org.omniai.sdk.contracts.openai.output.OpenAiChoice
import org.omniai.sdk.contracts.openai.output.OpenAiDelta
import org.omniai.sdk.contracts.openai.output.OpenAiEventStream
import org.omniai.sdk.contracts.openai.output.OpenAiMessageOutput
import org.omniai.sdk.contracts.openai.output.OpenAiToolCallFunctionOutput
import org.omniai.sdk.contracts.openai.output.OpenAiToolCallOutput
import org.omniai.sdk.domain.common.CommonRole
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.common.content.TextPart
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.requests.CommonRequestMessage
import org.omniai.sdk.domain.responses.TextDeltaEvent
import org.omniai.sdk.domain.responses.ToolCallArgumentsDeltaEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OpenAiAdapterTranslatorTest {
    private val translator = OpenAiOutboundTranslator()

    @Test
    fun `maps common request to openai request`() {
        val request =
            CommonRequest(
                provider = Provider.OPENAI,
                model = "gpt-4o-mini",
                messages =
                    listOf(
                        CommonRequestMessage(
                            role = CommonRole.USER,
                            content = listOf(TextPart("Hello")),
                        ),
                    ),
                config =
                    org.omniai.sdk.domain.common
                        .CommonGenerationConfig(maxTokens = 120),
            )

        val openAi = translator.fromDomain(request)

        assertEquals("gpt-4o-mini", openAi.model)
        assertEquals("user", openAi.messages.first().role)
        assertEquals("Hello", openAi.messages.first().content)
        assertEquals(120, openAi.maxTokens)
    }

    @Test
    fun `maps openai response to common response`() {
        val response =
            OpenAiChatCompletionsResponse(
                id = "chatcmpl_123",
                obj = "chat.completion",
                created = 1,
                model = "gpt-4o-mini",
                choices =
                    listOf(
                        OpenAiChoice(
                            index = 0,
                            message =
                                OpenAiMessageOutput(
                                    role = "assistant",
                                    content = "Done",
                                ),
                            finishReason = "stop",
                        ),
                    ),
            )

        val domain = translator.toDomain(response)

        assertEquals(Provider.OPENAI, domain.provider)
        assertEquals("chatcmpl_123", domain.id)
        assertEquals(
            "Done",
            (
                domain.choices
                    .first()
                    .message.content
                    .first() as TextPart
            ).text,
        )
    }

    @Test
    fun `maps openai chunk event to domain event`() =
        runTest {
            val event =
                OpenAiChatCompletionsResponse(
                    id = "chatcmpl_1",
                    obj = "chat.completion.chunk",
                    created = 2,
                    model = "gpt-4o-mini",
                    choices =
                        listOf(
                            OpenAiChoice(
                                index = 0,
                                delta = OpenAiDelta(content = "partial"),
                            ),
                        ),
                )

            val domainEvent =
                translator
                    .toDomainEvent(flowOf(OpenAiEventStream.Chunk(event)))
                    .first()

            val textDelta = assertIs<TextDeltaEvent>(domainEvent)
            assertEquals(Provider.OPENAI, textDelta.provider)
            assertEquals(Model("gpt-4o-mini"), textDelta.model)
        }

    @Test
    fun `maps tool call arguments JSON string to domain object`() {
        val response =
            OpenAiChatCompletionsResponse(
                id = "chatcmpl_123",
                obj = "chat.completion",
                created = 1,
                model = "gpt-4o-mini",
                choices =
                    listOf(
                        OpenAiChoice(
                            index = 0,
                            message =
                                OpenAiMessageOutput(
                                    role = "assistant",
                                    toolCalls =
                                        listOf(
                                            OpenAiToolCallOutput(
                                                id = "call_1",
                                                type = "function",
                                                function =
                                                    OpenAiToolCallFunctionOutput(
                                                        name = "get_weather",
                                                        arguments = "{\"city\":\"Lisboa\",\"unit\":\"celsius\"}",
                                                    ),
                                            ),
                                        ),
                                ),
                            finishReason = "tool_calls",
                        ),
                    ),
            )

        val domain = translator.toDomain(response)
        val part =
            domain.choices
                .first()
                .message.content[0] as org.omniai.sdk.domain.common.content.ToolCallPart
        assertEquals(
            "Lisboa",
            (part.argumentsJson["city"] as org.omniai.sdk.domain.common.json.JsonValue.JsonString).value,
        )
    }

    @Test
    fun `maps tool call argument fragment event from string`() =
        runTest {
            val event =
                OpenAiChatCompletionsResponse(
                    id = "chatcmpl_1",
                    obj = "chat.completion.chunk",
                    created = 2,
                    model = "gpt-4o-mini",
                    choices =
                        listOf(
                            OpenAiChoice(
                                index = 0,
                                delta =
                                    OpenAiDelta(
                                        toolCalls =
                                            listOf(
                                                OpenAiToolCallOutput(
                                                    id = "call_1",
                                                    index = 0,
                                                    type = "function",
                                                    function =
                                                        OpenAiToolCallFunctionOutput(
                                                            name = null,
                                                            arguments = "{\"city\":\"Lis",
                                                        ),
                                                ),
                                            ),
                                    ),
                            ),
                        ),
                )

            val domainEvent =
                translator
                    .toDomainEvent(flowOf(OpenAiEventStream.Chunk(event)))
                    .first()

            val argsDelta = assertIs<ToolCallArgumentsDeltaEvent>(domainEvent)
            assertTrue(argsDelta.argumentsFragment.startsWith("{\"city\""))
        }
}
