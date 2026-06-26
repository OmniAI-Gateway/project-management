package org.omniai.mcp.handler

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import org.omniai.mcp.domain.model.ContextDefinition

/**
 * Registers static Context entries as MCP Resources on the [Server].
 */
class ContextRegistrar {

    /**
     * Registers all [ContextDefinition] entries as MCP resources on the given [server].
     */
    fun registerContexts(server: Server, contexts: List<ContextDefinition>) {
        for (ctx in contexts) {
            server.addResource(
                uri = ctx.uri,
                name = ctx.name,
                description = ctx.description ?: "No description",
                mimeType = ctx.mimeType ?: "text/plain"
            ) {
                ReadResourceResult(
                    contents = listOf(
                        TextResourceContents(
                            text = ctx.content,
                            uri = ctx.uri,
                            mimeType = ctx.mimeType ?: "text/plain"
                        )
                    )
                )
            }
        }
    }
}
