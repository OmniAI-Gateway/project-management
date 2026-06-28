package org.omniai.mcp.client

import org.omniai.mcp.domain.model.McpClientConfig

/**
 * Manages connections to multiple external MCP servers.
 * Provides a registry of active connections, keyed by server name.
 */
class McpClientManager(
    private val transportFactory: ClientTransportFactory,
) {
    private val connections = mutableMapOf<String, McpClientConnection>()

    /**
     * Connects to a list of external MCP servers.
     * Disconnects any servers that are no longer present in the new list.
     */
    suspend fun syncConnections(clients: List<McpClientConfig>) {
        val newNames = clients.map { it.name }.toSet()

        // Disconnect removed servers
        val toRemove = connections.keys - newNames
        for (name in toRemove) {
            connections.remove(name)?.disconnect()
        }

        // Connect new servers
        for (client in clients) {
            if (client.name !in connections) {
                val connection = McpClientConnection(client, transportFactory)
                try {
                    connection.connect()
                    connections[client.name] = connection
                } catch (e: Exception) {
                    println("[McpClientManager] Failed to connect to '${client.name}': ${e.message}")
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
