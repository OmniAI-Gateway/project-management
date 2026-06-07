package org.omniai.sdk.adapters.anthropic

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.omniai.sdk.binders.IncomingContext
import org.omniai.sdk.contracts.anthropic.output.AnthropicError
import org.omniai.sdk.contracts.anthropic.output.AnthropicMessageResponse
import org.omniai.sdk.contracts.anthropic.output.AnthropicOutputContent
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamDelta
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent
import org.omniai.sdk.contracts.anthropic.output.AnthropicUsage
import org.omniai.sdk.common.Either
import org.omniai.sdk.common.TypedMap
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.input.RawText
import org.omniai.sdk.contracts.anthropic.output.MessageDeltaInfo
import org.omniai.sdk.ports.outbound.http.HttpCallResult
import org.omniai.sdk.ports.outbound.http.HttpMethod
import org.omniai.sdk.ports.outbound.http.HttpTransportClient
import org.omniai.sdk.ports.outbound.http.RequestConfig
import org.omniai.sdk.domain.common.CommonGenerationConfig
import org.omniai.sdk.domain.common.CommonRole
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.common.SystemPrompt
import org.omniai.sdk.domain.common.content.TextPart
import org.omniai.sdk.domain.errors.ApiDownError
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.requests.CommonRequestMessage
import org.omniai.sdk.domain.responses.ChoiceFinished
import org.omniai.sdk.domain.responses.ChoiceStarted
import org.omniai.sdk.domain.responses.ResponseCompleted
import org.omniai.sdk.domain.responses.ResponseErrored
import org.omniai.sdk.domain.responses.ResponseStarted
import org.omniai.sdk.domain.responses.TextDeltaEvent
import org.omniai.sdk.domain.responses.UsageReported

class AnthropicOutboundAdapterTest {

    // ==========================================
    // TESTES SÍNCRONOS (generate)
    // ==========================================

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

        val adapter = createAdapter(fakeClient)
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
    fun `generate maps complex request configuration accurately`() = runTest {
        val fakeClient = FakeHttpTransportClient().apply {
            executeResult = HttpCallResult.Success(
                AnthropicMessageResponse("msg_1", "message", "assistant", "claude-3-5-sonnet", emptyList())
            )
        }

        val complexRequest = CommonRequest(
            provider = Provider.ANTHROPIC,
            model = "claude-3-5-sonnet",
            systemPrompt = SystemPrompt("You are a helpful assistant"),
            config = CommonGenerationConfig(temperature = 0.5, maxTokens = 2048),
            messages = listOf(CommonRequestMessage(CommonRole.USER, listOf(TextPart("Hello"))))
        )

        val adapter = createAdapter(fakeClient)
        adapter.generate(complexRequest)

        val config = fakeClient.lastExecuteConfig
        val body = config?.body as? AnthropicMessagesRequest

        assertEquals(2048, body?.maxTokens)
        assertEquals(0.5, body?.temperature)
        assertEquals(RawText("You are a helpful assistant"), body?.system)
        assertEquals(null, body?.stream)
    }

    @Test
    fun `generate maps HttpCallResult ApiError to DomainError`() = runTest {
        val fakeClient = FakeHttpTransportClient().apply {
            executeResult = HttpCallResult.ApiError(code = 400, message = "Bad Request")
        }

        val adapter = createAdapter(fakeClient)
        val result = adapter.generate(commonRequest())

        assertTrue(result is Either.Left)
        // O tipo exacto depende de como o seu código lida com o Either.Left (ex: ProviderApiError ou InvalidRequest)
        assertTrue(result.value.message.contains("Bad Request"))
    }

    @Test
    fun `generate maps HttpCallResult NetworkError to DomainError`() = runTest {
        val fakeClient = FakeHttpTransportClient().apply {
            executeResult = HttpCallResult.NetworkError(RuntimeException("Connection refused"))
        }

        val adapter = createAdapter(fakeClient)
        val result = adapter.generate(commonRequest())

        assertTrue(result is Either.Left)
        assertTrue(result.value is ApiDownError)
    }

    // ==========================================
    // TESTES ASSÍNCRONOS / STREAMING (generateStream)
    // ==========================================

    @Test
    fun `generateStream maps complete successful stream lifecycle`() = runTest {
        // Simula o ciclo de vida completo de uma resposta de streaming da Anthropic
        val fakeClient = FakeHttpTransportClient().apply {
            listenResult = flowOf(
                HttpCallResult.Success(AnthropicStreamEvent.MessageStart(
                    AnthropicMessageResponse("msg_stream", "message", "assistant", "claude-3-5-sonnet"))),
                HttpCallResult.Success(AnthropicStreamEvent.ContentBlockStart(0,
                    AnthropicOutputContent.Text(""))),
                HttpCallResult.Success(AnthropicStreamEvent.ContentBlockDelta(0,
                    AnthropicStreamDelta.TextDelta("Hello"))),
                HttpCallResult.Success(AnthropicStreamEvent.ContentBlockStop(0)),
                HttpCallResult.Success(AnthropicStreamEvent.MessageDelta(
                    MessageDeltaInfo("end_turn"), AnthropicUsage(10, 5))),
                HttpCallResult.Success(AnthropicStreamEvent.MessageStop)
            )
        }

        val adapter = createAdapter(fakeClient)
        val result = adapter.generateStream(commonRequest())

        assertTrue(result is Either.Right)
        val events = result.value.toList()

        // Verifica se a sequência de eventos de domínio gerada é a correta
        assertTrue(events[0] is ResponseStarted)
        assertTrue(events[1] is ChoiceStarted)
        assertTrue(events[2] is TextDeltaEvent && (events[2] as TextDeltaEvent).text == "Hello")
        // Dependendo de como consertou o bug do translator, ChoiceFinished e UsageReported podem vir em ordem diferente ou omitir block_stop
        assertTrue(events.any { it is ChoiceFinished })
        assertTrue(events.any { it is UsageReported })
        assertTrue(events.last() is ResponseCompleted)
    }

    @Test
    fun `generateStream maps Anthropic specific inner error event to domain errored event`() = runTest {
        val fakeClient = FakeHttpTransportClient().apply {
            // A API responde HTTP 200, mas no meio do stream manda um evento de erro
            listenResult = flowOf(
                HttpCallResult.Success(AnthropicStreamEvent.Error(
                    AnthropicError("overloaded_error", "Server overloaded")))
            )
        }

        val adapter = createAdapter(fakeClient)
        val result = adapter.generateStream(commonRequest())

        assertTrue(result is Either.Right) // A conexão de stream abriu com sucesso
        val events = result.value.toList()

        assertTrue(events.first() is ResponseErrored)
        assertEquals("Server overloaded", (events.first() as ResponseErrored).message)
        assertTrue((events.first() as ResponseErrored).retryable) // overloaded costuma ser retryable
    }

    @Test
    fun `generateStream maps transport api error to domain error event`() = runTest {
        val fakeClient = FakeHttpTransportClient().apply {
            listenResult = flowOf(HttpCallResult.ApiError(code = 429, message = "rate limited"))
        }

        val adapter = createAdapter(fakeClient)
        val result = adapter.generateStream(commonRequest())

        assertTrue(result is Either.Right) // Fluxo emitido com sucesso
        val first = result.value.toList().first()
        assertTrue(first is ResponseErrored)
        assertTrue(first.message.contains("rate limited"))
    }

    @Test
    fun `generateStream maps transport api error without message to default message`() = runTest {
        val fakeClient = FakeHttpTransportClient().apply {
            listenResult = flowOf(HttpCallResult.ApiError(code = 500, message = null))
        }

        val adapter = createAdapter(fakeClient)
        val result = adapter.generateStream(commonRequest())

        assertTrue(result is Either.Right)
        val first = result.value.toList().first()
        assertTrue(first is ResponseErrored)
        assertTrue(first.message.contains("API error")) // Ou o seu fallback configurado
    }

    @Test
    fun `generateStream maps transport NetworkError to domain errored event`() = runTest {
        val fakeClient = FakeHttpTransportClient().apply {
            listenResult = flowOf(HttpCallResult.NetworkError(exception = RuntimeException("Read timeout")))
        }

        val adapter = createAdapter(fakeClient)
        val result = adapter.generateStream(commonRequest())

        assertTrue(result is Either.Right)
        val first = result.value.toList().first()
        assertTrue(first is ResponseErrored)
        assertTrue(first.message.contains("Read timeout") || first.message.contains("Network error"))
    }

    // ==========================================
    // FUNÇÕES AUXILIARES
    // ==========================================

    private fun createAdapter(fakeClient: FakeHttpTransportClient) = AnthropicOutboundAdapter(
        model = Model("claude-3-5-sonnet"),
        apiKey = "anthropic-key",
        baseUrl = "https://anthropic.local/v1",
        transportClient = fakeClient,
        anthropicVersion = "2023-06-01"
    )

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
    var listenResult: Flow<HttpCallResult<*>> = flowOf(HttpCallResult.Success(
        AnthropicStreamEvent.Error(AnthropicError("error", "not set"))))

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
