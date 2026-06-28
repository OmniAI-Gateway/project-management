package org.omniai.sdk.domain.common.json

import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsonValueConvertersTest {
    // ==========================================
    // 1. Raw Conversions (Any? <-> JsonValue)
    // ==========================================

    @Test
    fun `converts primitive values correctly`() {
        assertEquals(JsonValue.JsonString("x"), "x".toJsonValue())
        assertEquals(JsonValue.JsonBoolean(false), false.toJsonValue())
        assertEquals(JsonValue.JsonNumber(7), 7.toJsonValue())
        assertEquals(JsonValue.JsonNumber(3.14), 3.14.toJsonValue())
        assertEquals(JsonValue.JsonNull, null.toJsonValue())
    }

    @Test
    fun `converts unknown types using toString fallback`() {
        class CustomObject {
            override fun toString() = "custom_string"
        }

        val result = CustomObject().toJsonValue()

        assertIs<JsonValue.JsonString>(result)
        assertEquals("custom_string", result.value)
    }

    @Test
    fun `roundtrips deeply nested maps and lists seamlessly`() {
        val originalPayload =
            mapOf(
                "name" to "claude",
                "temperature" to 0.3,
                "enabled" to true,
                "tags" to listOf("ai", null, "gateway"), // Array com null
                "meta" to
                    mapOf(
                        "version" to 1,
                        "nested_list" to listOf(mapOf("id" to 10)), // Lista de mapas
                    ),
                "empty_map" to emptyMap<String, Any>(),
                "empty_list" to emptyList<Any>(),
            )

        // 1. Converte para o teu Domínio
        val jsonObject = originalPayload.toJsonObject()

        // 2. Validações estruturais intermédias
        assertIs<JsonValue.JsonString>(jsonObject.properties["name"])
        assertIs<JsonValue.JsonNumber>(jsonObject.properties["temperature"])
        assertIs<JsonValue.JsonArray>(jsonObject.properties["tags"])

        val tagsArray = jsonObject.properties["tags"] as JsonValue.JsonArray
        assertEquals(JsonValue.JsonNull, tagsArray.items[1]) // Verifica se o null passou bem

        // 3. Reverte para Raw e garante simetria (Round-trip)
        val raw = jsonObject.toRawMap()
        assertEquals(originalPayload, raw)
    }

    // ==========================================
    // 2. SimpleJsonParser (String -> JsonObject)
    // ==========================================

    @Test
    fun `parser converts valid complex JSON string to JsonObject`() {
        val jsonString =
            """
            {
                "model": "gpt-4",
                "stream": false,
                "max_tokens": 1024,
                "tools": [
                    { "type": "function", "name": "get_weather" }
                ],
                "metadata": null
            }
            """.trimIndent()

        val parsed = jsonString.toJsonObjectOrNull()
        assertNotNull(parsed)

        assertEquals("gpt-4", (parsed.properties["model"] as? JsonValue.JsonString)?.value)
        assertEquals(false, (parsed.properties["stream"] as? JsonValue.JsonBoolean)?.value)
        assertEquals(1024L, (parsed.properties["max_tokens"] as? JsonValue.JsonNumber)?.value)
        assertEquals(JsonValue.JsonNull, parsed.properties["metadata"])

        val toolsArray = parsed.properties["tools"] as? JsonValue.JsonArray
        assertNotNull(toolsArray)
        assertEquals(1, toolsArray.items.size)
    }

    @Test
    fun `parser handles escaped characters and unicode correctly`() {
        val jsonString =
            """
            { "text": "Linha 1\nLinha 2", "quote": "Ele disse \"olá\"", "unicode": "\u00A9" }
            """.trimIndent()

        val parsed = jsonString.toJsonObjectOrNull()
        assertNotNull(parsed)

        assertEquals("Linha 1\nLinha 2", (parsed.properties["text"] as? JsonValue.JsonString)?.value)
        assertEquals("Ele disse \"olá\"", (parsed.properties["quote"] as? JsonValue.JsonString)?.value)
        assertEquals("©", (parsed.properties["unicode"] as? JsonValue.JsonString)?.value)
    }

    @Test
    fun `parser fails gracefully and returns null for invalid JSON`() {
        val invalidStrings =
            listOf(
                """{ "name": "claude" """, // Falta fechar chaveta
                """{ "name": "claude", }""", // Vírgula extra
                """[ "array_is_not_object" ]""", // Não é um JsonObject na raiz
                """{ "bad_key": unquoted }""", // Valor não formatado
                "  ", // Vazio/Espaços
                "random text", // Lixo
            )

        invalidStrings.forEach { invalidJson ->
            assertNull(invalidJson.toJsonObjectOrNull(), "Should return null for: $invalidJson")
        }
    }

    // ==========================================
    // 3. Serialization (JsonValue -> String)
    // ==========================================

    @Test
    fun `toJsonString serializes objects and arrays correctly`() {
        val domainObj =
            JsonValue.JsonObject(
                mapOf(
                    "key" to JsonValue.JsonString("value"),
                    "number" to JsonValue.JsonNumber(42),
                    "list" to JsonValue.JsonArray(listOf(JsonValue.JsonBoolean(true), JsonValue.JsonNull)),
                ),
            )

        val jsonString = domainObj.toJsonString()

        // Remove os espaços caso a tua implementação os adicione, ou verifica a string compacta exata
        assertEquals("""{"key":"value","number":42,"list":[true,null]}""", jsonString)
    }

    @Test
    fun `toJsonString escapes inner quotes to prevent malformed JSON`() {
        // ATENÇÃO: Este teste vai falhar com a tua implementação atual do `toJsonString()` no JsonValue.kt!
        // O teu `is JsonString -> "\"$value\""` não escapa as aspas interiores.
        // É um excelente teste para te obrigar a corrigir o bug.
        val domainObj =
            JsonValue.JsonObject(
                mapOf(
                    "prompt" to JsonValue.JsonString("O utilizador disse \"olá\" ontem."),
                ),
            )

        val jsonString = domainObj.toJsonString()

        // Se não for escapado, a string gerada seria {"prompt":"O utilizador disse "olá" ontem."}, o que é JSON inválido.
        assertEquals("""{"prompt":"O utilizador disse \"olá\" ontem."}""", jsonString)
    }

    // ==========================================
    // 4. Kotlinx Serialization Bridge
    // ==========================================

    @Test
    fun `maintains strict symmetry when converting to and from kotlinx JsonElement`() {
        val originalPayload =
            mapOf(
                "system" to "You are an AI.",
                "metrics" to mapOf("latency" to 150.5, "success" to true),
                "flags" to listOf("experimental", null),
            ).toJsonObject()

        // Domínio -> Kotlinx
        val kotlinxElement = originalPayload.toKotlinxJsonElement()

        // Valida se o tipo do Kotlinx está correto
        assertTrue(kotlinxElement is JsonObject)

        // Kotlinx -> Domínio
        val restoredPayload = kotlinxElement.toDomainJsonValue()

        // Garante que o processo de ida e volta não destruiu dados nem mudou tipos
        assertEquals(originalPayload, restoredPayload)
    }
}
