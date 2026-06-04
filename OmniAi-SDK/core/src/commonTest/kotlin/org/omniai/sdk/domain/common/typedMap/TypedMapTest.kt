package org.omniai.sdk.domain.common.typedMap

import org.omniai.sdk.common.AttributeKey
import org.omniai.sdk.common.TypedMap
import org.omniai.sdk.common.key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertSame
import kotlin.test.assertNotSame
import kotlin.test.assertFailsWith

class TypedMapTest {

    @Test
    fun `stores and retrieves values using AttributeKey maintaining type safety`() {
        val map = TypedMap()
        val stringKey = key<String>("timeout")
        val intKey = key<Int>("retries")

        map[stringKey] = "30s"
        map[intKey] = 3

        assertEquals("30s", map[stringKey])
        assertEquals(3, map[intKey])
    }

    @Test
    fun `keys with exact same name but different types do not collide`() {
        val map = TypedMap()
        val stringKey = key<String>("metadata")
        val listKey = key<List<String>>("metadata")

        map[stringKey] = "uma string"
        map[listKey] = listOf("um", "array")

        assertEquals("uma string", map[stringKey])
        assertEquals(listOf("um", "array"), map[listKey])
        assertEquals(2, map.size())
    }

    @Test
    fun `reified string methods work correctly and interact with cache`() {
        val map = TypedMap()

        map.put("api_key", "sk-1234")
        map.put("max_tokens", 1024)

        assertTrue(map.contains<String>("api_key"))
        assertEquals("sk-1234", map.get<String>("api_key"))
        assertEquals(1024, map.get<Int>("max_tokens"))
    }

    @Test
    fun `companion object cache reuses identical key instances`() {
        val key1 = TypedMap.get("shared.key", String::class)
        val key2 = TypedMap.get("shared.key", String::class)

        assertSame(key1, key2)

        val differentTypeKey = TypedMap.get("shared.key", Int::class)
        assertNotSame<AttributeKey<*>>(key1, differentTypeKey)
    }

    @Test
    fun `require throws exception when key is missing`() {
        val map = TypedMap()
        val missingKey = key<String>("missing")

        val exception = assertFailsWith<IllegalStateException> {
            map.require(missingKey)
        }
        assertTrue(exception.message!!.contains("Missing required key: Key(missing: String)"))
    }

    @Test
    fun `getOrPut initializes value only once`() {
        val map = TypedMap()
        val counterKey = key<Int>("counter")

        var calls = 0

        val value1 = map.getOrPut(counterKey) {
            calls++
            100
        }

        val value2 = map.getOrPut(counterKey) {
            calls++
            200
        }

        assertEquals(100, value1)
        assertEquals(100, value2)
        assertEquals(1, calls, "A lambda de inicialização deve ser chamada apenas uma vez")
    }

    @Test
    fun `copy creates a deeply disconnected map`() {
        val original = TypedMap()
        val hostKey = key<String>("host")
        original[hostKey] = "localhost"
        val copy = original.copy()
        copy[hostKey] = "production"
        assertEquals("localhost", original[hostKey])
        assertEquals("production", copy[hostKey])
    }

    @Test
    fun `putAll merges correctly`() {
        val map1 = TypedMap()
        map1.put("k1", "v1")
        val map2 = TypedMap()
        map2.put("k2", "v2")
        map2.put("k1", "overridden")
        map1.putAll(map2)
        assertEquals("overridden", map1.get<String>("k1"))
        assertEquals("v2", map1.get<String>("k2"))
        assertEquals(2, map1.size())
    }
}