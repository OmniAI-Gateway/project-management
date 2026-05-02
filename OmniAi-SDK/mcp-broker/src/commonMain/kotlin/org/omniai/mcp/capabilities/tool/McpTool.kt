package org.omniai.mcp.capabilities.tool

import kotlinx.serialization.KSerializer
import org.omniai.mcp.schema.generateToolSchema

abstract class McpTool<T : Any>(
    val name: String,
    val description: String,
    val serializer: KSerializer<T>
) {
    val schema: ToolSchemaDefinition by lazy {
        generateToolSchema(serializer.descriptor)
    }

    abstract suspend fun execute(input: T): ToolResult
}
