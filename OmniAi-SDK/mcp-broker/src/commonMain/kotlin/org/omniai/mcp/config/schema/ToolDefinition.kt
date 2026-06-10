package org.omniai.mcp.config.schema

import kotlinx.serialization.Serializable

/**
 * Describes a single dynamically configured MCP tool.
 *
 * @property name Unique tool name exposed via MCP.
 * @property description Human-readable description for the LLM.
 * @property inputSchema JSON Schema definition of the tool's input parameters.
 * @property http HTTP execution details (URL, method, headers, body mapping).
 */
@Serializable
data class ToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: InputSchemaDefinition,
    val http: HttpExecutionDefinition
)

/**
 * JSON Schema-style definition of a tool's input parameters.
 *
 * @property properties Map of parameter name to property schema definition.
 * @property required List of parameter names that are mandatory.
 */
@Serializable
data class InputSchemaDefinition(
    val properties: Map<String, PropertyDefinition>,
    val required: List<String> = emptyList()
)

/**
 * Schema definition for a single property, supporting nested objects and arrays.
 *
 * @property type JSON Schema type: "string", "integer", "number", "boolean", "object", "array".
 * @property description Optional human-readable description for the LLM.
 * @property items For "array" type: schema definition of array elements.
 * @property properties For "object" type: nested property definitions.
 * @property required For "object" type: list of required nested property names.
 */
@Serializable
data class PropertyDefinition(
    val type: String,
    val description: String? = null,
    val items: PropertyDefinition? = null,
    val properties: Map<String, PropertyDefinition>? = null,
    val required: List<String>? = null
)

/**
 * HTTP execution configuration for a tool invocation.
 *
 * @property url Target URL, supports `{paramName}` placeholders resolved from input args.
 * @property method HTTP method string: "GET", "POST", "PUT", "DELETE", "PATCH".
 * @property headers Static HTTP headers (e.g., auth tokens, API keys).
 * @property bodyMapping Optional body construction rules. If null and method supports body, no body is sent.
 */
@Serializable
data class HttpExecutionDefinition(
    val url: String,
    val method: String = "GET",
    val headers: Map<String, String> = emptyMap(),
    val bodyMapping: BodyMappingDefinition? = null
)

/**
 * Controls how the HTTP request body is constructed from tool input arguments.
 *
 * @property contentType MIME type for the request body.
 * @property template Optional field mapping. If null, all input arguments are forwarded as-is as JSON.
 */
@Serializable
data class BodyMappingDefinition(
    val contentType: String = "application/json",
    val template: BodyTemplate? = null
)

/**
 * Explicit field mapping for the request body.
 *
 * @property fields Map of target JSON field name → source argument name from the tool input.
 */
@Serializable
data class BodyTemplate(
    val fields: Map<String, String>
)
