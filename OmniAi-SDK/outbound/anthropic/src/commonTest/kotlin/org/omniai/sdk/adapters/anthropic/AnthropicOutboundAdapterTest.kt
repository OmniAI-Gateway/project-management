package org.omniai.sdk.adapters.anthropic

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.omniai.sdk.contracts.anthropic.output.AnthropicError
import org.omniai.sdk.contracts.anthropic.output.AnthropicMessageResponse
import org.omniai.sdk.contracts.anthropic.output.AnthropicOutputContent
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent
import org.omniai.sdk.core.commom.Either
import org.omniai.sdk.core.http.HttpCallResult
import org.omniai.sdk.core.http.HttpMethod
import org.omniai.sdk.core.http.HttpTransportClient
import org.omniai.sdk.core.http.RequestConfig
import org.omniai.sdk.domain.common.CommonRole
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.common.content.TextPart
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.requests.CommonRequestMessage
import org.omniai.sdk.domain.responses.ResponseCompleted
import org.omniai.sdk.domain.responses.ResponseErrored
import org.omniai.sdk.domain.responses.ResponseStarted

class AnthropicOutboundAdapterTest {

    @Test
    fun `generate sends expected request and maps success`() = runTest {
        val fakeClient = FakeHttpTransportClient().apply {
            executeResult = HttpCallResult.Success(
                AnthropicMessageResponse(
                    id = "msg_1",
                    type = "message",
                    role = "assistant",
                    model = "claude-3-5-sonnet",
                    content = listOf(AnthropicOutputContent.Text("Done"))
                )
            )
        }

        val adapter = AnthropicOutboundAdapter(
            model = Model("claude-3-5-sonnet"),
            apiKey = "anthropic-key",
            baseUrl = "https://anthropic.local/v1",
            transportClient = fakeClient,
            anthropicVersion = "2023-06-01"
        )

        val result = adapter.generate(commonRequest())

        assertTrue(result is Either.Right)
        assertEquals("msg_1", result.value.id)

        val config = fakeClient.lastExecuteConfig
        assertEquals("https://anthropic.local/v1/messages", config?.url)
        assertEquals(HttpMethod.POST, config?.method)
        assertEquals("anthropic-key", config?.headers?.get("x-api-key")?.first())
        assertEquals("2023-06-01", config?.headers?.get("anthropic-version")?.first())
        assertEquals("application/json", config?.headers?.get("content-type")?.first())
    }

    @Test
    fun `generateStream enables stream and maps message start plus completion`() = runTest {
        val fakeClient = FakeHttpTransportClient().apply {
            listenResult = flowOf(
                HttpCallResult.Success(
                    AnthropicStreamEvent.MessageStart(
                        message = AnthropicMessageResponse(
                            id = "msg_stream",
                            type = "message",
                            role = "assistant",
                            model = "claude-3-5-sonnet"
                        )
                    )
                )
            )
        }

        val adapter = AnthropicOutboundAdapter(
            model = Model("claude-3-5-sonnet"),
            apiKey = "anthropic-key",
            baseUrl = "https://anthropic.local/v1",
            transportClient = fakeClient,
            anthropicVersion = "2023-06-01"
        )

        val result = adapter.generateStream(commonRequest())

        assertTrue(result is Either.Right)
        val events = result.value.toList()
        assertTrue(events.first() is ResponseStarted)
        assertTrue(events.last() is ResponseCompleted)

        val config = fakeClient.lastListenManyConfig
        assertEquals("https://anthropic.local/v1/messages", config?.url)
        val body = config?.body as? org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
        assertEquals(true, body?.stream)
    }

    @Test
    fun `generateStream maps transport api error to domain error event`() = runTest {
        val fakeClient = FakeHttpTransportClient().apply {
            listenResult = flowOf(HttpCallResult.ApiError(code = 429, message = "rate limited"))
        }

        val adapter = AnthropicOutboundAdapter(
            model = Model("claude-3-5-sonnet"),
            apiKey = "anthropic-key",
            baseUrl = "https://anthropic.local/v1",
            transportClient = fakeClient,
            anthropicVersion = "2023-06-01"
        )

        val result = adapter.generateStream(commonRequest())

        assertTrue(result is Either.Right)
        val first = result.value.toList().first()
        assertTrue(first is ResponseErrored)
        assertTrue(first.message.contains("rate limited"))
    }

    @Test
    fun `generateStream maps transport api error without message to default message`() = runTest {
        val fakeClient = FakeHttpTransportClient().apply {
            listenResult = flowOf(HttpCallResult.ApiError(code = 500, message = null))
        }

        val adapter = AnthropicOutboundAdapter(
            model = Model("claude-3-5-sonnet"),
            apiKey = "anthropic-key",
            baseUrl = "https://anthropic.local/v1",
            transportClient = fakeClient,
            anthropicVersion = "2023-06-01"
        )

        val result = adapter.generateStream(commonRequest())

        assertTrue(result is Either.Right)
        val first = result.value.toList().first()
        assertTrue(first is ResponseErrored)
        assertTrue(first.message.contains("API error"))
    }

    private fun commonRequest(): CommonRequest = CommonRequest(
        provider = Provider.ANTHROPIC,
        model = "claude-3-5-sonnet",
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
    var listenResult: Flow<HttpCallResult<*>> = flowOf(HttpCallResult.Success(AnthropicStreamEvent.Error(AnthropicError("error", "not set"))))

    var lastExecuteConfig: RequestConfig<*>? = null
    var lastListenConfig: RequestConfig<*>? = null
    var lastListenManyConfig: RequestConfig<*>? = null

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
