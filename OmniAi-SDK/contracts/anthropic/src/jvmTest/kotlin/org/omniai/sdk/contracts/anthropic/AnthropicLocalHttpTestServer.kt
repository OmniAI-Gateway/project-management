package org.omniai.sdk.contracts.anthropic

import com.sun.net.httpserver.Headers
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.Executors

internal data class CapturedRequest(
    val method: String,
    val path: String,
    val headers: Headers,
    val body: String
)

internal class AnthropicLocalHttpTestServer(
    private val responseBody: String
) : AutoCloseable {

    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val executor = Executors.newSingleThreadExecutor()

    @Volatile
    private var lastRequest: CapturedRequest? = null

    val baseUrl: String
        get() = "http://127.0.0.1:${server.address.port}"

    val capturedRequest: CapturedRequest?
        get() = lastRequest

    val capturedRequestBody: String?
        get() = lastRequest?.body

    init {
        server.executor = executor
        server.createContext("/") { exchange ->
            val body = exchange.requestBody.bufferedReader().use { it.readText() }
            lastRequest = CapturedRequest(
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
