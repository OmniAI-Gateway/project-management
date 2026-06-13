package org.omniai.mcp.gateway.client

import org.omniai.mcp.domain.BrokerServerClient

/**
 * Manages connections to multiple external MCP servers.
 * Provides a registry of active connections, keyed by server name.
 */
class McpClientManager(
    private val transportFactory: McpTransportFactory
) {
    private val connections = mutableMapOf<String, McpClientConnection>()

    /**
     * Connects to a list of external MCP servers.
     * Disconnects any servers that are no longer present in the new list.
     */
    suspend fun syncConnections(servers: List<BrokerServerClient>) {
        val newNames = servers.map { it.name }.toSet()

        // Disconnect removed servers
        val toRemove = connections.keys - newNames
        for (name in toRemove) {
            connections.remove(name)?.disconnect()
        }

        // Connect new servers
        for (server in servers) {
            if (server.name !in connections) {
                val connection = McpClientConnection(server)
                try {
                    connection.connect(transportFactory)
                    connections[server.name] = connection
                } catch (e: Exception) {
                    println("[McpClientManager] Failed to connect to '${server.name}': ${e.message}")
                }
            }
        }
    }

    /**
     * Returns all currently active connections.
     */
    fun getConnections(): Map<String, McpClientConnection> = connections.toMap()

    /**
     * Returns a specific connection by server name.
     */
    fun getConnection(serverName: String): McpClientConnection? = connections[serverName]

    /**
     * Disconnects from all external servers.
     */
    suspend fun disconnectAll() {
        for ((_, connection) in connections) {
            connection.disconnect()
        }
        connections.clear()
    }
}
