package org.omniai.sdk.domain.common.http_client

import org.omniai.sdk.ports.outbound.http.HttpMethod
import org.omniai.sdk.ports.outbound.http.requestConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class RequestConfigDslTest {
    @Test
    fun `should build RequestConfig correctly using DSL`() {
        val config =
            requestConfig("https://api.provider.com/{resource}") {
                method = HttpMethod.POST
                numberOfTries = 5
                body = "{\"prompt\": \"Hello\"}"

                pathParam("resource", "completions")

                header("Authorization", "Bearer token123")
                header("Accept", "application/json")

                // Test parameter accumulation with same key
                parameter("filter", "active")
                parameter("filter", "recent")
            }

        assertEquals("https://api.provider.com/completions", config.url)
        assertEquals(HttpMethod.POST, config.method)
        assertEquals(5, config.numberOfTries)
        assertEquals("{\"prompt\": \"Hello\"}", config.body)

        // Verify headers
        assertEquals(listOf("Bearer token123"), config.headers["Authorization"])
        assertEquals(listOf("application/json"), config.headers["Accept"])

        // Verify multiple query params
        assertEquals(listOf("active", "recent"), config.queryParams["filter"])
    }
}
