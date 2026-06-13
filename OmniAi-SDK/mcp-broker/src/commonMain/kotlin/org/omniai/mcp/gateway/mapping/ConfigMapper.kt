package org.omniai.mcp.gateway.mapping

import org.omniai.mcp.domain.BrokerContext
import org.omniai.mcp.domain.BrokerServerClient
import org.omniai.mcp.domain.BrokerTool
import org.omniai.mcp.dto.BrokerConfigDto
import org.omniai.mcp.dto.ContextConfigDto
import org.omniai.mcp.dto.McpServerConfigDto
import org.omniai.mcp.dto.ToolConfigDto

/**
 * Maps DTOs from the YAML configuration to internal Domain models.
 */
class ConfigMapper {

    fun mapTool(dto: ToolConfigDto): BrokerTool {
        return BrokerTool(
            name = dto.name,
            description = dto.description,
            targetUrl = dto.targetUrl,
            method = dto.method,
            headers = dto.headers ?: emptyMap(),
            inputSchema = dto.inputSchema
        )
    }

    fun mapContext(dto: ContextConfigDto): BrokerContext {
        return BrokerContext(
            name = dto.name,
            uri = dto.uri,
            description = dto.description,
            mimeType = dto.mimeType,
            content = dto.content
        )
    }

    fun mapServerClient(dto: McpServerConfigDto): BrokerServerClient {
        return BrokerServerClient(
            name = dto.name,
            transport = BrokerServerClient.TransportType.valueOf(dto.transport.name),
            command = dto.command,
            args = dto.args,
            url = dto.url
        )
    }

    fun mapConfig(dto: BrokerConfigDto): MappedConfig {
        return MappedConfig(
            tools = dto.tools.map { mapTool(it) },
            contexts = dto.contexts.map { mapContext(it) },
            servers = dto.mcpServers.map { mapServerClient(it) }
        )
    }
}
