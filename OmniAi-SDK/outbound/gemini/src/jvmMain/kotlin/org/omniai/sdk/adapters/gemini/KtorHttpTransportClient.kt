package org.omniai.sdk.adapters.gemini

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod as KtorHttpMethod
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.sse.ServerSentEvent
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.omniai.sdk.core.http.HttpCallResult
import org.omniai.sdk.core.http.HttpMethod
import org.omniai.sdk.core.http.HttpTransportClient
import org.omniai.sdk.core.http.RequestConfig

internal class KtorHttpTransportClient(
    private val client: HttpClient,
    private val json: Json = defaultJson(),
    private val retryDelay: Duration = 1.seconds
) : HttpTransportClient {

    override suspend fun <T, V> execute(
        config: RequestConfig<V>,
        responseSerializer: KSerializer<T>
    ): HttpCallResult<T> {
        val maxTries = config.numberOfTries.coerceAtLeast(1)

        repeat(maxTries) { attempt ->
            try {
                val response = client.request(config.url) { applyConfig(config) }

                if (!response.status.isSuccess()) {
                    return HttpCallResult.ApiError(response.status.value, response.bodyAsText())
                }

                val responseBody = response.bodyAsText()
                if (responseBody.isBlank() && responseSerializer.isUnitSerializer()) {
                    @Suppress("UNCHECKED_CAST")
                    return HttpCallResult.Success(Unit as T)
                }

                val parsed = json.decodeFromString(responseSerializer, responseBody)
                return HttpCallResult.Success(parsed)
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception

                if (shouldRetry(exception, attempt, maxTries)) {
                    delay(retryDelay)
                } else {
                    return exception.toCallResult()
                }
            }
        }

        return HttpCallResult.UnknownError(IllegalStateException("Retry limit reached"))
    }

    override fun <T> listen(
        config: RequestConfig<Unit>,
        eventName: String?,
        responseSerializer: KSerializer<T>
    ): Flow<HttpCallResult<T>> = flow {
        client.sse(
            urlString = config.url,
            request = { applyConfig(config) }
        ) {
            incoming.collect { event ->
                processSingleEvent(event, eventName, responseSerializer, json)?.let { emit(it) }
            }
        }
    }.retry(retries = config.numberOfTries.toLong().coerceAtLeast(1L) - 1L) { cause ->
        if (cause is IOException) {
            delay(retryDelay)
            true
        } else {
            false
        }
    }.catch { cause ->
        if (cause is CancellationException) throw cause
        emit(cause.toCallResult())
    }

    override fun <E : Any> listenMany(
        config: RequestConfig<Unit>,
        serializersByEvent: Map<String, KSerializer<out E>>
    ): Flow<HttpCallResult<E>> = flow {
        client.sse(
            urlString = config.url,
            request = { applyConfig(config) }
        ) {
            incoming.collect { event ->
                val serializer = serializersByEvent[event.event ?: return@collect] ?: return@collect
                val data = event.data ?: return@collect

                try {
                    val parsed = json.decodeFromString(serializer, data)
                    emit(HttpCallResult.Success(parsed))
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                    emit(exception.toCallResult())
                }
            }
        }
    }.retry(retries = config.numberOfTries.toLong().coerceAtLeast(1L) - 1L) { cause ->
        if (cause is IOException) {
            delay(retryDelay)
            true
        } else {
            false
        }
    }.catch { cause ->
        if (cause is CancellationException) throw cause
        emit(cause.toCallResult())
    }

    companion object {
        fun default(): KtorHttpTransportClient =
            KtorHttpTransportClient(
                client = HttpClient(OkHttp) {
                    engine {
                        config {
                            pingInterval(30, TimeUnit.SECONDS)
                        }
                    }
                    install(HttpTimeout) {
                        connectTimeoutMillis = 10_000
                        socketTimeoutMillis = Long.MAX_VALUE
                        requestTimeoutMillis = Long.MAX_VALUE
                    }
                    install(ContentNegotiation) {
                        json(defaultJson())
                    }
                    install(SSE)
                }
            )

        private fun defaultJson(): Json =
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = false
            }
    }
}

private fun <V> HttpRequestBuilder.applyConfig(config: RequestConfig<V>) {
    method = config.method.toKtorMethod()
    url {
        config.queryParams.forEach { (key, values) ->
            values.forEach { value -> parameters.append(key, value) }
        }
    }
    config.headers.forEach { (key, values) ->
        values.forEach { value -> headers.append(key, value) }
    }
    if (config.body != null) {
        contentType(ContentType.Application.Json)
        setBody(config.body as Any)
    }
}

private fun KSerializer<*>.isUnitSerializer(): Boolean = descriptor == Unit.serializer().descriptor

private fun shouldRetry(exception: Exception, attempt: Int, maxTries: Int): Boolean =
    exception is IOException && attempt < maxTries - 1

private fun Throwable.toCallResult(): HttpCallResult<Nothing> =
    when (this) {
        is SerializationException -> HttpCallResult.SerializationError(this)
        is IOException -> HttpCallResult.NetworkError(this)
        else -> HttpCallResult.UnknownError(Exception(this))
    }

private fun <T> processSingleEvent(
    event: ServerSentEvent,
    targetEventName: String?,
    serializer: KSerializer<T>,
    json: Json
): HttpCallResult<T>? {
    val data = event.data ?: return null
    if (targetEventName != null && targetEventName != event.event) return null

    return try {
        HttpCallResult.Success(json.decodeFromString(serializer, data))
    } catch (exception: Exception) {
        if (exception is CancellationException) throw exception
        exception.toCallResult()
    }
}

private fun HttpMethod.toKtorMethod(): KtorHttpMethod =
    when (this) {
        HttpMethod.GET -> KtorHttpMethod.Get
        HttpMethod.POST -> KtorHttpMethod.Post
        HttpMethod.PUT -> KtorHttpMethod.Put
        HttpMethod.DELETE -> KtorHttpMethod.Delete
        HttpMethod.PATCH -> KtorHttpMethod.Patch
        HttpMethod.HEAD -> KtorHttpMethod.Head
        HttpMethod.OPTIONS -> KtorHttpMethod.Options
    }

