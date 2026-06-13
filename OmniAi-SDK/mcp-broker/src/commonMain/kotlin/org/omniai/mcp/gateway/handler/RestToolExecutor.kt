package org.omniai.mcp.gateway.handler

import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import org.omniai.mcp.domain.BrokerTool

/**
 * Executes HTTP requests for REST API tools defined in YAML.
 */
class RestToolExecutor(
    private val httpClient: HttpClient
) {
    /**
     * Calls the target REST API and returns the raw response body as a string.
     */
    suspend fun execute(tool: BrokerTool, arguments: Map<String, Any?>): String {
        val response = httpClient.request(tool.targetUrl) {
            method = when (tool.method.uppercase()) {
                "GET" -> HttpMethod.Get
                "POST" -> HttpMethod.Post
                "PUT" -> HttpMethod.Put
                "DELETE" -> HttpMethod.Delete
                "PATCH" -> HttpMethod.Patch
                else -> HttpMethod.Get
            }
            headers {
                tool.headers.forEach { (key, value) -> append(key, value) }
            }
            if (tool.method.uppercase() in listOf("POST", "PUT", "PATCH") && arguments.isNotEmpty()) {
                contentType(ContentType.Application.Json)
                setBody(arguments)
            }
        }
        return response.bodyAsText()
    }
}
