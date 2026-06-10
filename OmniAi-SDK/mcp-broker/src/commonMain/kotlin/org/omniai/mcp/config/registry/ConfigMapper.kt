package org.omniai.mcp.config.registry

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.builtins.serializer
import org.omniai.mcp.capabilities.resource.McpResource
import org.omniai.mcp.capabilities.resource.ResourceContent
import org.omniai.mcp.capabilities.tool.ToolSchemaDefinition
import org.omniai.mcp.config.schema.InputSchemaDefinition
import org.omniai.mcp.config.schema.PropertyDefinition
import org.omniai.mcp.config.schema.ResourceDefinition
import org.omniai.sdk.ports.outbound.http.HttpCallResult
import org.omniai.sdk.ports.outbound.http.HttpTransportClient
import org.omniai.sdk.ports.outbound.http.RequestConfig

/**
 * Maps parsed YAML schema definitions into the MCP domain types
 * ([ToolSchemaDefinition], [McpResource]).
 *
 * Defined as an interface to support testing with alternative implementations
 * and to maintain clean hexagonal boundaries.
 */
interface ConfigMapper {

    /**
     * Converts an [InputSchemaDefinition] from YAML into a [ToolSchemaDefinition]
     * compatible with the MCP tool registration.
     */
    fun mapToolSchema(inputSchema: InputSchemaDefinition): ToolSchemaDefinition

    /**
     * Converts a single [PropertyDefinition] into a JSON Schema [JsonObject].
     * Handles recursive nesting for "object" and "array" types.
     */
    fun mapPropertyToJsonSchema(property: PropertyDefinition): JsonObject

    /**
     * Creates an [McpResource] from a [ResourceDefinition], wiring the resource handler
     * to fetch data via the given [HttpTransportClient].
     */
    fun mapResource(definition: ResourceDefinition, httpClient: HttpTransportClient): McpResource
}

/**
 * Default implementation of [ConfigMapper].
 */
class DefaultConfigMapper : ConfigMapper {

    override fun mapToolSchema(inputSchema: InputSchemaDefinition): ToolSchemaDefinition {
        val properties = inputSchema.properties.mapValues { (_, prop) ->
            mapPropertyToJsonSchema(prop)
        }
        return ToolSchemaDefinition(
            properties = JsonObject(properties),
            required = inputSchema.required.takeIf { it.isNotEmpty() }
        )
    }

    override fun mapPropertyToJsonSchema(property: PropertyDefinition): JsonObject {
        val map = mutableMapOf<String, JsonElement>()
        map["type"] = JsonPrimitive(property.type)

        property.description?.let { map["description"] = JsonPrimitive(it) }

        // Handle "array" → nested items schema
        if (property.type == "array" && property.items != null) {
            map["items"] = mapPropertyToJsonSchema(property.items)
        }

        // Handle "object" → nested properties schema
        if (property.type == "object" && property.properties != null) {
            map["properties"] = JsonObject(
                property.properties.mapValues { (_, p) -> mapPropertyToJsonSchema(p) }
            )
            property.required?.takeIf { it.isNotEmpty() }?.let { req ->
                map["required"] = JsonArray(req.map { JsonPrimitive(it) })
            }
        }

        return JsonObject(map)
    }

    override fun mapResource(
        definition: ResourceDefinition,
        httpClient: HttpTransportClient
    ): McpResource {
        return McpResource(
            uri = definition.uriTemplate,
            name = definition.name,
            description = definition.description,
            mimeType = definition.mimeType,
            handler = { uri ->
                // Extract path params by matching the concrete URI against the template
                val params = extractUriParams(definition.uriTemplate, uri)
                val resolvedUrl = resolveUrlWithParams(definition.fetch.url, params)

                val config = RequestConfig<String>(
                    url = resolvedUrl,
                    method = parseHttpMethod(definition.fetch.method),
                    headers = definition.fetch.headers.mapValues { listOf(it.value) }
                )

                val result = httpClient.execute(config, String.serializer())
                when (result) {
                    is HttpCallResult.Success -> ResourceContent(
                        uri = uri,
                        mimeType = definition.mimeType,
                        text = result.data
                    )
                    is HttpCallResult.ApiError -> ResourceContent(
                        uri = uri,
                        mimeType = "text/plain",
                        text = "HTTP ${result.code}: ${result.message}"
                    )
                    is HttpCallResult.NetworkError -> ResourceContent(
                        uri = uri,
                        mimeType = "text/plain",
                        text = "Network error: ${result.exception.message}"
                    )
                    is HttpCallResult.SerializationError -> ResourceContent(
                        uri = uri,
                        mimeType = "text/plain",
                        text = "Parse error: ${result.exception.message}"
                    )
                    is HttpCallResult.UnknownError -> ResourceContent(
                        uri = uri,
                        mimeType = "text/plain",
                        text = "Unknown error: ${result.exception.message}"
                    )
                }
            }
        )
    }
}
