package org.omniai.sdk.domain.common.binders

import org.omniai.sdk.binders.client.bindClientResponseMetadata
import org.omniai.sdk.common.key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClientResponseMetadataBinderTest {

    @Test
    fun shouldBindDynamicHeaderNamesCaseInsensitivelyAndStandardProperties() {
        val dynamicHeaders = setOf("X-RateLimit-Remaining", "Authorization")

        val context = FakeIncomingContext(
            headers = mapOf(
                "X-RateLimit-Remaining" to "99",
                "Authorization" to "Bearer token123",
                "Unrequested-Header" to "ShouldBeIgnored"
            ),
            properties = mapOf(
                "statusCode" to "200",
                "url" to "https://api.omniai.org/v1/chat"
            )
        )

        // Call to ClientResponseMetadataBinder.kt function
        val result = bindClientResponseMetadata(context, dynamicHeaders)

        // Verifies the lowercase logic in header keys generation
        val rateLimitKey = key<String>("http.header.x-ratelimit-remaining")
        val authKey = key<String>("http.header.authorization")

        assertEquals("99", result.get(rateLimitKey))
        assertEquals("Bearer token123", result.get(authKey))

        // Verifies predefined properties (statusCode converted to Int)
        val statusKey = key<Int>("http.statusCode")
        val urlKey = key<String>("http.url")

        assertEquals(200, result.get(statusKey))
        assertEquals("https://api.omniai.org/v1/chat", result.get(urlKey))

        // Ensures unrequested headers were not mistakenly mapped
        val ignoredHeaderKey = key<String>("http.header.unrequested-header")
        assertNull(result.get(ignoredHeaderKey))
    }
}