package org.omniai.mcp.gateway.mapping

import kotlinx.serialization.json.*
import net.mamoe.yamlkt.YamlElement
import net.mamoe.yamlkt.YamlList
import net.mamoe.yamlkt.YamlLiteral
import net.mamoe.yamlkt.YamlMap
import net.mamoe.yamlkt.YamlNull
import org.omniai.mcp.domain.BrokerContext
import org.omniai.mcp.domain.BrokerServerClient
import org.omniai.mcp.domain.BrokerTool
import org.omniai.mcp.dto.BrokerConfigDto
import org.omniai.mcp.dto.ContextConfigDto
import org.omniai.mcp.dto.McpServerConfigDto
import org.omniai.mcp.dto.ToolConfigDto

fun YamlElement.toJsonElement(): JsonElement = when (this) {
    is YamlNull -> JsonNull
    is YamlLiteral -> {
        val content = this.content
        when {
            content == "true" -> JsonPrimitive(true)
            content == "false" -> JsonPrimitive(false)
            content.toIntOrNull() != null -> JsonPrimitive(content.toInt())
            content.toDoubleOrNull() != null -> JsonPrimitive(content.toDouble())
            else -> JsonPrimitive(content)
        }
    }
    is YamlMap -> buildJsonObject {
        this@toJsonElement.content.forEach { (k, v) ->
            put(k.content.toString(), v.toJsonElement())
        }
    }
    is YamlList -> buildJsonArray {
        this@toJsonElement.content.forEach { add(it.toJsonElement()) }
    }
    else -> JsonNull
}

fun YamlMap?.toJsonObject(): JsonObject? {
    if (this == null) return null
    return this.toJsonElement() as? JsonObject
}

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
            pathSchema = dto.pathSchema.toJsonObject(),
            querySchema = dto.querySchema.toJsonObject(),
            bodySchema = dto.bodySchema.toJsonObject()
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
