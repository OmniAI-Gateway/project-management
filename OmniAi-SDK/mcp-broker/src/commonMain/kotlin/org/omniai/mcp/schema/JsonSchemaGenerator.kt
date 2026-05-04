package org.omniai.mcp.schema

import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.omniai.mcp.capabilities.tool.ToolSchemaDefinition

fun generateToolSchema(descriptor: SerialDescriptor): ToolSchemaDefinition {
    val properties = mutableMapOf<String, JsonElement>()
    val required = mutableListOf<String>()

    for (i in 0 until descriptor.elementsCount) {
        val name = descriptor.getElementName(i)
        val elementDescriptor = descriptor.getElementDescriptor(i)

        // In Kotlin Serialization:
        // isOptional means it has a default value.
        // elementDescriptor.isNullable means it accepts null.
        val isRequired = !descriptor.isElementOptional(i) && !elementDescriptor.isNullable

        properties[name] = buildPropertySchema(elementDescriptor)

        if (isRequired) {
            required.add(name)
        }
    }

    return ToolSchemaDefinition(
        properties = JsonObject(properties),
        required = required.takeIf { it.isNotEmpty() }
    )
}

private fun buildPropertySchema(descriptor: SerialDescriptor): JsonObject {
    val typeName = when (descriptor.kind) {
        PrimitiveKind.STRING -> "string"
        PrimitiveKind.INT, PrimitiveKind.LONG, PrimitiveKind.SHORT, PrimitiveKind.BYTE -> "integer"
        PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> "number"
        PrimitiveKind.BOOLEAN -> "boolean"
        else -> "string" // Fallback or handle Object/Array recursively
    }
    return JsonObject(mapOf("type" to JsonPrimitive(typeName)))
}
