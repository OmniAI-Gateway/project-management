package org.omniai.mcp.config.registry

import org.omniai.mcp.capabilities.resource.McpResource
import org.omniai.mcp.capabilities.tool.McpTool
import org.omniai.mcp.config.parsing.ConfigParser
import org.omniai.sdk.ports.outbound.http.HttpTransportClient

/**
 * Orchestrates the full dynamic registration pipeline:
 * YAML parsing → schema mapping → MCP tool/resource creation.
 *
 * @param parser The [ConfigParser] used to deserialize the YAML content.
 * @param configMapper The [ConfigMapper] used to translate schema definitions into domain types.
 * @param httpClient The [HttpTransportClient] injected into dynamically created tools and resources.
 */
class DynamicToolRegistry(
    private val parser: ConfigParser,
    private val configMapper: ConfigMapper,
    private val httpClient: HttpTransportClient
) {

    /**
     * Parses the given YAML content and creates all defined tools and resources.
     *
     * @param yamlContent Raw YAML configuration string.
     * @return [DynamicRegistrationResult] containing the created tools and resources,
     *         ready to be registered with [org.omniai.mcp.core.McpServerBuilder].
     */
    fun loadFromYaml(yamlContent: String): DynamicRegistrationResult {
        val config = parser.parse(yamlContent)

        val tools = config.tools.map { toolDef ->
            val schema = configMapper.mapToolSchema(toolDef.inputSchema)
            DynamicMcpTool(
                name = toolDef.name,
                description = toolDef.description,
                toolDefinition = toolDef,
                httpClient = httpClient,
                schemaDefinition = schema
            )
        }

        val resources = config.resources.map { resDef ->
            configMapper.mapResource(resDef, httpClient)
        }

        return DynamicRegistrationResult(tools, resources)
    }
}

/**
 * Result of loading a dynamic YAML configuration.
 *
 * @property tools List of dynamically created MCP tools.
 * @property resources List of dynamically created MCP resources.
 */
data class DynamicRegistrationResult(
    val tools: List<McpTool<*>>,
    val resources: List<McpResource>
)
