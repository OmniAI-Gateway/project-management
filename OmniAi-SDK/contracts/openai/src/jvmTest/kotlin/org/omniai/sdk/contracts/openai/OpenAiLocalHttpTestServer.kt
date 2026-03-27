package org.omniai.sdk.contracts.openai

import com.sun.net.httpserver.Headers
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.Executors

internal data class OpenAiCapturedRequest(
    val method: String,
    val path: String,
    val headers: Headers,
    val body: String
)

internal class OpenAiLocalHttpTestServer(
    private val responseBody: String
) : AutoCloseable {

    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val executor = Executors.newSingleThreadExecutor()

    @Volatile
    private var lastRequest: OpenAiCapturedRequest? = null

    val baseUrl: String
        get() = "http://127.0.0.1:${server.address.port}"

    val capturedRequest: OpenAiCapturedRequest?
        get() = lastRequest

    val capturedRequestBody: String?
        get() = lastRequest?.body

    init {
        server.executor = executor
        server.createContext("/") { exchange ->
            val body = exchange.requestBody.bufferedReader().use { it.readText() }
            lastRequest = OpenAiCapturedRequest(
                method = exchange.requestMethod,
                path = exchange.requestURI.path,
                headers = exchange.requestHeaders,
                body = body
            )
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, responseBody.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(responseBody.toByteArray()) }
        }
    }

    fun start() {
        server.start()
    }

    override fun close() {
        server.stop(0)
        executor.shutdown()
    }
}

