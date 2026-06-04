package org.omniai.sdk.domain.common.json

fun Any?.toJsonValue(): JsonValue =
    when (this) {
        null -> JsonValue.JsonNull
        is JsonValue -> this
        is String -> JsonValue.JsonString(this)
        is Boolean -> JsonValue.JsonBoolean(this)
        is Number -> JsonValue.JsonNumber(this)
        is Map<*, *> -> JsonValue.JsonObject(
            properties = entries.associate { (key, value) ->
                key.toString() to value.toJsonValue()
            }
        )
        is List<*> -> JsonValue.JsonArray(items = map { it.toJsonValue() })
        else -> JsonValue.JsonString(toString())
    }

fun Map<String, Any?>.toJsonObject(): JsonValue.JsonObject =
    JsonValue.JsonObject(properties = mapValues { (_, value) -> value.toJsonValue() })

fun JsonValue.toRawAny(): Any? =
    when (this) {
        is JsonValue.JsonNull -> null
        is JsonValue.JsonString -> value
        is JsonValue.JsonBoolean -> value
        is JsonValue.JsonNumber -> value
        is JsonValue.JsonArray -> items.map { it.toRawAny() }
        is JsonValue.JsonObject -> properties.mapValues { (_, value) -> value.toRawAny() }
    }

fun JsonValue.JsonObject.toRawMap(): Map<String, Any?> =
    properties.mapValues { (_, value) -> value.toRawAny() }



fun String.toJsonObjectOrNull(): JsonValue.JsonObject? {
    val trimmed = this.trim()
    if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null

    return try {
        SimpleJsonParser(trimmed).parse() as? JsonValue.JsonObject
    } catch (_: Exception) {
        null
    }
}

private class SimpleJsonParser(private val input: String) {
    private var pos = 0

    private fun skipWhitespace() {
        while (pos < input.length && input[pos].isWhitespace()) pos++
    }

    fun parse(): JsonValue {
        skipWhitespace()
        if (pos >= input.length) throw IllegalArgumentException("Fim inesperado")

        return when (input[pos]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't' -> parseLiteral("true", JsonValue.JsonBoolean(true))
            'f' -> parseLiteral("false", JsonValue.JsonBoolean(false))
            'n' -> parseLiteral("null", JsonValue.JsonNull)
            else -> parseNumber()
        }
    }

    private fun parseObject(): JsonValue.JsonObject {
        pos++ // Ignora o '{'
        val map = mutableMapOf<String, JsonValue>()
        skipWhitespace()
        if (input[pos] == '}') {
            pos++
            return JsonValue.JsonObject(map)
        }
        while (true) {
            skipWhitespace()
            val key = parseString().value
            skipWhitespace()
            if (input[pos] != ':') throw IllegalArgumentException("Faltou ':'")
            pos++
            val value = parse()
            map[key] = value

            skipWhitespace()
            if (input[pos] == '}') {
                pos++
                break
            }
            if (input[pos] != ',') throw IllegalArgumentException("Faltou ','")
            pos++
        }
        return JsonValue.JsonObject(map)
    }

    private fun parseArray(): JsonValue.JsonArray {
        pos++ // Ignora o '['
        val list = mutableListOf<JsonValue>()
        skipWhitespace()
        if (input[pos] == ']') {
            pos++
            return JsonValue.JsonArray(list)
        }
        while (true) {
            list.add(parse())
            skipWhitespace()
            if (input[pos] == ']') {
                pos++
                break
            }
            if (input[pos] != ',') throw IllegalArgumentException("Faltou ','")
            pos++
        }
        return JsonValue.JsonArray(list)
    }

    private fun parseString(): JsonValue.JsonString {
        pos++ // Ignora a primeira aspa '"'
        val sb = StringBuilder()
        while (pos < input.length) {
            val c = input[pos++]
            if (c == '"') return JsonValue.JsonString(sb.toString())

            // Lógica para lidar com texto escapado (Ex: \" )
            if (c == '\\') {
                when (val esc = input[pos++]) {
                    '"', '\\', '/' -> sb.append(esc)
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        // Lida com caracteres unicode (ex: \u00A9)
                        sb.append(input.substring(pos, pos + 4).toInt(16).toChar())
                        pos += 4
                    }
                }
            } else {
                sb.append(c)
            }
        }
        throw IllegalArgumentException("String não terminada")
    }

    private fun parseNumber(): JsonValue.JsonNumber {
        val start = pos
        while (pos < input.length && (input[pos].isDigit() || input[pos] in listOf('-', '+', '.', 'e', 'E'))) {
            pos++
        }
        val numStr = input.substring(start, pos)

        val isIntegral = !numStr.contains('.') && !numStr.contains('e', ignoreCase = true)

        return if (isIntegral) {
            JsonValue.JsonNumber(numStr.toLongOrNull() ?: numStr.toDouble())
        } else {
            JsonValue.JsonNumber(numStr.toDouble())
        }
    }

    private fun parseLiteral(literal: String, value: JsonValue): JsonValue {
        if (input.startsWith(literal, pos)) {
            pos += literal.length
            return value
        }
        throw IllegalArgumentException("Literal inválido")
    }
}