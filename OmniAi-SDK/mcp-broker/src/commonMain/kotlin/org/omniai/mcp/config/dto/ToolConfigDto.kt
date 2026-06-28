package org.omniai.mcp.config.dto

import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.YamlMap

/**
 * DTO for configuring an external REST API as an MCP Tool.
 */
@Serializable
data class ToolConfigDto(
    val name: String,
    val description: String? = null,
    val targetUrl: String,
    val method: String = "GET",
    val headers: Map<String, String>? = null,
    val pathSchema: YamlMap? = null,
    val querySchema: YamlMap? = null,
    val bodySchema: YamlMap? = null,
)
