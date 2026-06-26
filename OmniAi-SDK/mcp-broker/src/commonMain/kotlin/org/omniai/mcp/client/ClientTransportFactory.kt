package org.omniai.mcp.client

import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import org.omniai.mcp.domain.model.McpClientConfig

/**
 * Interface for creating MCP client transports based on server configuration.
 */
interface ClientTransportFactory {
    fun create(config: McpClientConfig): Transport
}
