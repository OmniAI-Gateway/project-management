package org.omniai.sdk.adapters.openai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.omniai.sdk.binders.IncomingContext
import org.omniai.sdk.binders.buildMetadataBinder
import org.omniai.sdk.contracts.openai.output.OpenAiChatCompletionsResponse
import org.omniai.sdk.contracts.openai.output.OpenAiChoice
import org.omniai.sdk.contracts.openai.output.OpenAiDelta
import org.omniai.sdk.contracts.openai.output.OpenAiMessageOutput
import org.omniai.sdk.core.commom.Either
import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.core.commom.key
import org.omniai.sdk.core.http.HttpCallResult
import org.omniai.sdk.core.http.HttpMethod
import org.omniai.sdk.core.http.HttpTransportClient
import org.omniai.sdk.core.http.RequestConfig
import org.omniai.sdk.domain.common.CommonRole
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.common.content.TextPart
import org.omniai.sdk.domain.errors.InvalidRequest
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.requests.CommonRequestMessage
import org.omniai.sdk.domain.responses.ResponseCompleted
import org.omniai.sdk.domain.responses.TextDeltaEvent

class OpenAiOutboundAdapterTest {

    @Test
    fun `generate sends expected request and maps success`() = runTest {
        val fakeClient = FakeHttpTransportClient().apply {
            executeResult = HttpCallResult.Success(
                OpenAiChatCompletionsResponse(
                    id = "chatcmpl_1",
                    obj = "chat.completion",
                    created = 1,
                    model = "gpt-4o-mini",
                    choices = listOf(
                        OpenAiChoice(
                            index = 0,
                            message = OpenAiMessageOutput(role = "assistant", content = "Done")
                        )
                    )
                )
            )
        }

        val adapter = OpenAiOutboundAdapter(
            model = Model("gpt-4o-mini"),
            apiKey = "sk-test",
            baseUrl = "https://openai.local/v1",
            transportClient = fakeClient
        )

        val result = adapter.generate(commonRequest())

        assertTrue(result is Either.Right)
        assertEquals("chatcmpl_1", result.value.id)

        val config = fakeClient.lastExecuteConfig
        assertEquals("https://openai.local/v1/chat/completions", config?.url)
        assertEquals(HttpMethod.POST, config?.method)
        assertEquals("Bearer sk-test", config?.headers?.get("Authorization")?.first())
        assertEquals("application/json", config?.headers?.get("Content-Type")?.first())
    }

    @Test
    fun `generate maps api error to invalid request`() = runTest {
        val fakeClient = FakeHttpTransportClient().apply {
            executeResult = HttpCallResult.ApiError(code = 401, message = "invalid token")
        }

        val adapter = OpenAiOutboundAdapter(
            model = Model("gpt-4o-mini"),
            apiKey = "sk-test",
            baseUrl = "https://openai.local/v1",
            transportClient = fakeClient
        )

        val result = adapter.generate(commonRequest())

        assertTrue(result is Either.Left)
        assertTrue(result.value is InvalidRequest)
    }

    @Test
    fun `generateStream enables stream and maps chunk plus done event`() = runTest {
        val fakeClient = FakeHttpTransportClient().apply {
            listenResult = flowOf(
                HttpCallResult.Success(
                    OpenAiChatCompletionsResponse(
                        id = "chatcmpl_stream",
                        obj = "chat.completion.chunk",
                        created = 2,
                        model = "gpt-4o-mini",
                        choices = listOf(OpenAiChoice(index = 0, delta = OpenAiDelta(content = "partial")))
                    )
                )
            )
        }

        val adapter = OpenAiOutboundAdapter(
            model = Model("gpt-4o-mini"),
            apiKey = "sk-test",
            baseUrl = "https://openai.local/v1",
            transportClient = fakeClient
        )

        val result = adapter.generateStream(commonRequest())

        assertTrue(result is Either.Right)
        val events = result.value.toList()
        assertTrue(events.first() is TextDeltaEvent)
        assertTrue(events.last() is ResponseCompleted)

        val config = fakeClient.lastListenConfig
        assertEquals("https://openai.local/v1/chat/completions", config?.url)
        assertEquals("Bearer sk-test", config?.headers?.get("Authorization")?.first())

        val body = config?.body as? org.omniai.sdk.contracts.openai.input.OpenAiChatCompletionsRequest
        assertEquals(true, body?.stream)
    }

    @Test
    fun `generate merges response metadata into provider options`() = runTest {
        val requestIdKey = key<String>("http.requestId")
        val metadata = TypedMap().apply { put(requestIdKey, "req-123") }

        val fakeClient = FakeHttpTransportClient().apply {
            executeResult = HttpCallResult.Success(
                OpenAiChatCompletionsResponse(
                    id = "chatcmpl_meta",
                    obj = "chat.completion",
                    created = 1,
                    model = "gpt-4o-mini",
                    choices = listOf(
                        OpenAiChoice(
                            index = 0,
                            message = OpenAiMessageOutput(role = "assistant", content = "Done")
                        )
                    )
                ),
                metadata = metadata
            )
        }

        val binder = buildMetadataBinder {
            header("x-request-id") bindTo requestIdKey
        }

        val adapter = OpenAiOutboundAdapter(
            model = Model("gpt-4o-mini"),
            apiKey = "sk-test",
            baseUrl = "https://openai.local/v1",
            transportClient = fakeClient,
        )

        val result = adapter.generate(commonRequest())

        assertTrue(result is Either.Right)
        assertEquals("req-123", result.value.providerOptions["http.requestId"])
    }

    private fun commonRequest(): CommonRequest = CommonRequest(
        provider = Provider.OPENAI,
        model = "gpt-4o-mini",
        messages = listOf(
            CommonRequestMessage(
                role = CommonRole.USER,
                content = listOf(TextPart("Hello"))
            )
        )
    )
}

private class FakeHttpTransportClient : HttpTransportClient {
    var executeResult: HttpCallResult<*> = HttpCallResult.UnknownError(IllegalStateException("executeResult not set"))
    var listenResult: Flow<HttpCallResult<*>> = flowOf(HttpCallResult.UnknownError(IllegalStateException("listenResult not set")))

    var lastExecuteConfig: RequestConfig<*>? = null
    var lastListenConfig: RequestConfig<*>? = null
    var lastListenManyConfig: RequestConfig<*>? = null

    override fun bindResponseMetadata(context: IncomingContext, headerNames: Set<String>): TypedMap = TypedMap()

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T, V> execute(config: RequestConfig<V>, responseSerializer: KSerializer<T>): HttpCallResult<T> {
        lastExecuteConfig = config
        return executeResult as HttpCallResult<T>
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T, V> listen(config: RequestConfig<V>, eventName: String?, responseSerializer: KSerializer<T>): Flow<HttpCallResult<T>> {
        lastListenConfig = config
        return listenResult as Flow<HttpCallResult<T>>
    }

    @Suppress("UNCHECKED_CAST")
    override fun <E : Any, V> listenMany(config: RequestConfig<V>, serializersByEvent: Map<String, KSerializer<out E>>): Flow<HttpCallResult<E>> {
        lastListenManyConfig = config
        return listenResult as Flow<HttpCallResult<E>>
    }
}
