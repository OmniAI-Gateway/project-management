package org.omniai.sdk.adapters.gemini

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.omniai.sdk.binders.IncomingContext
import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.contracts.gemini.output.GeminiCandidate
import org.omniai.sdk.contracts.gemini.output.GeminiGenerateContentResponse
import org.omniai.sdk.contracts.gemini.output.GeminiResponseContent
import org.omniai.sdk.contracts.gemini.output.GeminiResponsePart
import org.omniai.sdk.core.commom.Either
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

class GeminiOutboundAdapterTest {

    @Test
    fun `generate sends expected request and maps success`() = runTest {
        val fakeClient = FakeHttpTransportClient().apply {
            executeResult = HttpCallResult.Success(
                GeminiGenerateContentResponse(
                    candidates = listOf(
                        GeminiCandidate(
                            index = 0,
                            content = GeminiResponseContent(parts = listOf(GeminiResponsePart(text = "Done")))
                        )
                    ),
                    modelVersion = "gemini-2.0-flash",
                    responseId = "resp_1"
                )
            )
        }

        val adapter = GeminiOutboundAdapter(
            model = Model("gemini-2.0-flash"),
            apiKey = "gemini-key",
            baseUrl = "https://gemini.local/v1beta",
            transportClient = fakeClient
        )

        val result = adapter.generate(commonRequest())

        assertTrue(result is Either.Right)
        assertEquals("resp_1", result.value.id)

        val config = fakeClient.lastExecuteConfig
        assertEquals("https://gemini.local/v1beta/models/gemini-2.0-flash:generateContent", config?.url)
        assertEquals(HttpMethod.POST, config?.method)
        assertEquals("gemini-key", config?.headers?.get("x-goog-api-key")?.first())
        assertEquals("application/json", config?.headers?.get("content-type")?.first())
        assertEquals("gemini-key", config?.queryParams?.get("key")?.first())
    }

    @Test
    fun `generate maps api error to invalid request`() = runTest {
        val fakeClient = FakeHttpTransportClient().apply {
            executeResult = HttpCallResult.ApiError(code = 400, message = "invalid argument")
        }

        val adapter = GeminiOutboundAdapter(
            model = Model("gemini-2.0-flash"),
            apiKey = "gemini-key",
            baseUrl = "https://gemini.local/v1beta",
            transportClient = fakeClient
        )

        val result = adapter.generate(commonRequest())

        assertTrue(result is Either.Left)
        assertTrue(result.value is InvalidRequest)
    }

    @Test
    fun `generateStream sends sse params and maps chunk plus done`() = runTest {
        val fakeClient = FakeHttpTransportClient().apply {
            listenResult = flowOf(
                HttpCallResult.Success(
                    GeminiGenerateContentResponse(
                        candidates = listOf(
                            GeminiCandidate(
                                index = 0,
                                content = GeminiResponseContent(parts = listOf(GeminiResponsePart(text = "partial")))
                            )
                        ),
                        modelVersion = "gemini-2.0-flash",
                        responseId = "resp_stream"
                    )
                )
            )
        }

        val adapter = GeminiOutboundAdapter(
            model = Model("gemini-2.0-flash"),
            apiKey = "gemini-key",
            baseUrl = "https://gemini.local/v1beta",
            transportClient = fakeClient
        )

        val result = adapter.generateStream(commonRequest())

        assertTrue(result is Either.Right)
        val events = result.value.toList()
        assertTrue(events.first() is TextDeltaEvent)
        assertTrue(events.last() is ResponseCompleted)

        val config = fakeClient.lastListenConfig
        assertEquals(
            "https://gemini.local/v1beta/models/gemini-2.0-flash:streamGenerateContent",
            config?.url
        )
        assertEquals("gemini-key", config?.queryParams?.get("key")?.first())
        assertEquals("sse", config?.queryParams?.get("alt")?.first())
    }

    private fun commonRequest(): CommonRequest = CommonRequest(
        provider = Provider.GEMINI,
        model = "gemini-2.0-flash",
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
