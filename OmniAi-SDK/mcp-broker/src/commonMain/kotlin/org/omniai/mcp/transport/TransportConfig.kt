package org.omniai.mcp.transport

sealed interface TransportConfig
class StdioTransportConfig : TransportConfig
class SseTransportConfig(val port: Int, val path: String, val messagePath: String) : TransportConfig
class WebSocketTransportConfig(val port: Int, val path: String) : TransportConfig
