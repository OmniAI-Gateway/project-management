package org.omniai.mcp.gateway.handler

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.omniai.mcp.domain.BrokerTool

/**
 * Registers REST API tools on the MCP [Server].
 */
class ToolRegistrar(
    private val restToolExecutor: RestToolExecutor
) {
    /**
     * Registers all [BrokerTool] entries as MCP tools on the given [server].
     */
    fun registerTools(server: Server, tools: List<BrokerTool>) {
        for (tool in tools) {
            val inputSchema = buildToolSchema(tool)
            server.addTool(
                name = tool.name,
                description = tool.description ?: "No description",
                inputSchema = inputSchema
            ) { request ->
                val arguments = request.params.arguments?.mapValues { 
                    val value = it.value
                    if (value is JsonPrimitive && value.isString) value.content else value 
                } ?: emptyMap()
                try {
                    val result = restToolExecutor.execute(tool, arguments)
                    CallToolResult(content = listOf(TextContent(result)))
                } catch (e: Exception) {
                    CallToolResult(
                        content = listOf(TextContent("Error calling tool '${tool.name}': ${e.message}")),
                        isError = true
                    )
                }
            }
        }
    }

    private fun buildToolSchema(tool: BrokerTool): ToolSchema {
        val schema = tool.inputSchema
        val properties = buildJsonObject {
            schema?.forEach { (paramName, paramType) ->
                put(paramName, buildJsonObject {
                    put("type", JsonPrimitive(paramType))
                })
            }
        }
        val requiredList = schema?.keys?.toList()
        return ToolSchema(
            properties = properties,
            required = requiredList
        )
    }
}
