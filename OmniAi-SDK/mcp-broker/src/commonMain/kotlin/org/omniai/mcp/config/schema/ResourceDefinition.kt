package org.omniai.mcp.config.schema

import kotlinx.serialization.Serializable

/**
 * Describes a single dynamically configured MCP resource.
 *
 * @property name Human-readable resource name.
 * @property uriTemplate MCP URI template, e.g. "api://users/{userId}".
 * @property description Optional description for discovery.
 * @property mimeType Expected MIME type of the fetched content.
 * @property fetch HTTP fetch configuration for retrieving the resource data.
 */
@Serializable
data class ResourceDefinition(
    val name: String,
    val uriTemplate: String,
    val description: String? = null,
    val mimeType: String? = null,
    val fetch: FetchDefinition
)

/**
 * HTTP fetch configuration for a resource.
 *
 * @property url Actual HTTP URL with `{param}` placeholders resolved from the MCP URI.
 * @property method HTTP method (typically "GET").
 * @property headers Static HTTP headers for authentication or content negotiation.
 */
@Serializable
data class FetchDefinition(
    val url: String,
    val method: String = "GET",
    val headers: Map<String, String> = emptyMap()
)
