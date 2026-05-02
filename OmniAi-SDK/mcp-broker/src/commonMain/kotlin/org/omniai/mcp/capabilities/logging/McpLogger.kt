package org.omniai.mcp.capabilities.logging

interface McpLogger {
    suspend fun emit(level: McpLogLevel, data: String, loggerName: String? = null)
}
