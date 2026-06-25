package org.omniai.mcp.gateway.handler

import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonArray
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
        // Path arguments are those defined in pathSchema
        val pathKeys = tool.pathSchema?.keys ?: emptySet()
        val pathArguments = arguments.filterKeys { it in pathKeys }

        // Substitui os path arguments no URL
        val resolvedUrl = pathArguments.entries.fold(tool.targetUrl) { url, (key, value) ->
            url.replace("{$key}", value?.toString() ?: "")
        }

        val response = httpClient.request(resolvedUrl) {
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

            // Query arguments: anexados ao URL via URLBuilder do Ktor
            val queryKeys = tool.querySchema?.keys ?: emptySet()
            queryKeys.forEach { queryKey ->
                if (arguments.containsKey(queryKey)) {
                    url.parameters.append(queryKey, arguments[queryKey]?.toString() ?: "")
                }
            }

            // Body arguments: enviados no corpo do pedido JSON
            val bodyKeys = tool.bodySchema?.keys ?: emptySet()
            val bodyArguments = arguments.filterKeys { it in bodyKeys }

            if (tool.method.uppercase() in listOf("POST", "PUT", "PATCH") && bodyArguments.isNotEmpty()) {
                contentType(ContentType.Application.Json)
                // Usa a nova função recursiva para converter os dados
                val jsonBody = buildJsonObject {
                    bodyArguments.forEach { (k, v) -> put(k, v.toJsonElement()) }
                }
                setBody(jsonBody)
            }
        }
        return response.bodyAsText()
    }
}

private fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is String -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Map<*, *> -> buildJsonObject {
        forEach { (k, v) -> put(k.toString(), v.toJsonElement()) }
    }
    is Iterable<*> -> buildJsonArray {
        forEach { add(it.toJsonElement()) }
    }
    is Array<*> -> buildJsonArray {
        forEach { add(it.toJsonElement()) }
    }
    else -> JsonPrimitive(this.toString())
}
