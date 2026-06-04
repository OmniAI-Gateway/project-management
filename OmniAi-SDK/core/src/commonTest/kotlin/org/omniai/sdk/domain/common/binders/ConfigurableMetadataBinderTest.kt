package org.omniai.sdk.domain.common.binders

import org.omniai.sdk.binders.buildMetadataBinder
import org.omniai.sdk.common.key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConfigurableMetadataBinderTest {

    private val intKey = key<Int>("test.int")
    private val stringKey = key<String>("test.string")
    private val boolKey = key<Boolean>("test.boolean")

    @Test
    fun shouldBindValidValuesFromContextToTypedMap() {
        val context = FakeIncomingContext(
            headers = mapOf("X-Limit" to "100"),
            properties = mapOf("Is-Active" to "true"),
            pathParams = mapOf("id" to "user-123")
        )

        val binder = buildMetadataBinder {
            header("X-Limit").bindToInt(intKey)
            property("Is-Active").bindToBoolean(boolKey)
            path("id") bindTo stringKey
        }

        val result = binder.bind(context)

        // Assuming your TypedMap has a get(AttributeKey) function
        assertEquals(100, result.get(intKey))
        assertEquals(true, result.get(boolKey))
        assertEquals("user-123", result.get(stringKey))
    }

    @Test
    fun shouldIgnoreMissingValuesInIncomingContext() {
        // Empty context, values will return null
        val context = FakeIncomingContext()

        val binder = buildMetadataBinder {
            header("X-Limit").bindToInt(intKey)
            property("Is-Active").bindToBoolean(boolKey)
        }

        val result = binder.bind(context)

        assertNull(result[intKey], "Keys not present in context should be ignored")
        assertNull(result[boolKey])
    }

    @Test
    fun shouldGracefullyHandleValuesThatFailToDecode() {
        // Context with data that cannot be converted to expected types
        val context = FakeIncomingContext(
            headers = mapOf("X-Limit" to "Not-A-Number"),
            properties = mapOf("Is-Active" to "Not-A-Boolean")
        )

        val binder = buildMetadataBinder {
            header("X-Limit").bindToInt(intKey)
            property("Is-Active").bindToBoolean(boolKey)
        }

        val result = binder.bind(context)

        // Decoding fails silently (toIntOrNull returns null)
        // BindingSpec ensures null values are not written to the target
        assertNull(result[intKey], "Malformed types should not crash extraction")
        assertNull(result[boolKey])
    }
}