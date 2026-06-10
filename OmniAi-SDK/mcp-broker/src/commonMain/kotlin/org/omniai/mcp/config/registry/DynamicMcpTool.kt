package org.omniai.mcp.config.registry

import kotlinx.serialization.json.JsonObject
import org.omniai.mcp.capabilities.tool.McpTool
import org.omniai.mcp.capabilities.tool.ToolContent
import org.omniai.mcp.capabilities.tool.ToolResult
import org.omniai.mcp.capabilities.tool.ToolSchemaDefinition
import org.omniai.mcp.config.schema.ToolDefinition
import org.omniai.sdk.ports.outbound.http.HttpCallResult
import org.omniai.sdk.ports.outbound.http.HttpTransportClient
import org.omniai.sdk.ports.outbound.http.RequestConfig
import kotlinx.serialization.builtins.serializer

/**
 * A dynamically configured MCP tool that executes HTTP calls based on YAML configuration.
 *
 * Uses [JsonObject] as the input type since the schema is defined at runtime (not a compile-time
 * `@Serializable` class). The [HttpTransportClient] from the core module handles the actual
 * HTTP execution, keeping this class platform-agnostic and testable.
 *
 * @param name Tool name exposed via MCP.
 * @param description Human-readable description for the LLM.
 * @param toolDefinition The parsed YAML tool definition containing HTTP execution rules.
 * @param httpClient The shared HTTP client for executing requests.
 * @param schemaDefinition Pre-mapped JSON schema for the tool's input parameters.
 */
class DynamicMcpTool(
    name: String,
    description: String,
    private val toolDefinition: ToolDefinition,
    private val httpClient: HttpTransportClient,
    schemaDefinition: ToolSchemaDefinition
) : McpTool<JsonObject>(name, description, JsonObject.serializer()) {

    // Override the lazy schema from the parent with the YAML-derived one
    override val schema: ToolSchemaDefinition = schemaDefinition

    override suspend fun execute(input: JsonObject): ToolResult {
        // 1. Extract path params from input args that match {placeholder} names in URL
        val pathParams = extractPathParams(toolDefinition.http.url, input)

        // 2. Resolve the URL template with extracted path params
        val resolvedUrl = resolveUrlWithParams(toolDefinition.http.url, pathParams)

        // 3. Build the request body from input based on bodyMapping rules
        val body = buildRequestBody(toolDefinition.http.bodyMapping, input)

        // 4. Construct RequestConfig using core's types
        val config = RequestConfig(
            url = resolvedUrl,
            method = parseHttpMethod(toolDefinition.http.method),
            headers = toolDefinition.http.headers.mapValues { listOf(it.value) },
            body = body
        )

        // 5. Execute via the shared HttpTransportClient, expecting raw String response
        // 6. Map HttpCallResult → ToolResult
        return when (val result = httpClient.execute(config, String.serializer())) {
            is HttpCallResult.Success ->
                ToolResult(content = listOf(ToolContent.Text(result.data)))

            is HttpCallResult.ApiError ->
                ToolResult(
                    content = listOf(ToolContent.Text("HTTP ${result.code}: ${result.message}")),
                    isError = true
                )

            is HttpCallResult.NetworkError ->
                ToolResult(
                    content = listOf(ToolContent.Text("Network error: ${result.exception.message}")),
                    isError = true
                )

            is HttpCallResult.SerializationError ->
                ToolResult(
                    content = listOf(ToolContent.Text("Parse error: ${result.exception.message}")),
                    isError = true
                )

            is HttpCallResult.UnknownError ->
                ToolResult(
                    content = listOf(ToolContent.Text("Unknown error: ${result.exception.message}")),
                    isError = true
                )
        }
    }
}
