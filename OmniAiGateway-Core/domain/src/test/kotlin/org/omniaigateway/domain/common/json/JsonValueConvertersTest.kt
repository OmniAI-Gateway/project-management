package org.omniaigateway.domain.common.json

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class JsonValueConvertersTest {

    @Test
    fun `converts map to json object and back`() {
        val payload = mapOf(
            "name" to "claude",
            "temperature" to 0.3,
            "enabled" to true,
            "tags" to listOf("ai", "gateway"),
            "meta" to mapOf("version" to 1)
        )

        val jsonObject = payload.toJsonObject()

        assertIs<JsonValue.JsonString>(jsonObject.properties.getValue("name"))
        assertIs<JsonValue.JsonNumber>(jsonObject.properties.getValue("temperature"))
        assertIs<JsonValue.JsonBoolean>(jsonObject.properties.getValue("enabled"))
        assertIs<JsonValue.JsonArray>(jsonObject.properties.getValue("tags"))
        assertIs<JsonValue.JsonObject>(jsonObject.properties.getValue("meta"))

        val raw = jsonObject.toRawMap()

        assertEquals("claude", raw["name"])
        assertEquals(0.3, raw["temperature"])
        assertEquals(true, raw["enabled"])
        assertEquals(listOf("ai", "gateway"), raw["tags"])
    }

    @Test
    fun `converts primitive values`() {
        assertEquals(JsonValue.JsonString("x"), "x".toJsonValue())
        assertEquals(JsonValue.JsonBoolean(false), false.toJsonValue())
        assertEquals(JsonValue.JsonNumber(7), 7.toJsonValue())
        assertEquals(JsonValue.JsonNull, null.toJsonValue())
    }
}

