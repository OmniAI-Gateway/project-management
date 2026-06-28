package org.omniai.sdk.domain.common.httpClient

import org.omniai.sdk.ports.outbound.http.resolveUrlTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UrlTemplateResolverTest {
    @Test
    fun `should replace path parameters correctly`() {
        val rawUrl = "https://api.provider.com/v1/models/{modelId}/generate/{action}"
        val params =
            mapOf(
                "modelId" to "gpt-4",
                "action" to "stream",
            )

        val result = resolveUrlTemplate(rawUrl, params)

        assertEquals("https://api.provider.com/v1/models/gpt-4/generate/stream", result)
    }

    @Test
    fun `should throw exception when there are unresolved placeholders`() {
        val rawUrl = "https://api.provider.com/v1/models/{modelId}/generate"
        val params = emptyMap<String, String>() // Forgot to pass modelId

        val exception =
            assertFailsWith<IllegalArgumentException> {
                resolveUrlTemplate(rawUrl, params)
            }

        assertEquals("Unresolved path parameters in URL: https://api.provider.com/v1/models/{modelId}/generate", exception.message)
    }

    @Test
    fun `should not change url if there are no placeholders`() {
        val rawUrl = "https://api.provider.com/v1/models"
        val result = resolveUrlTemplate(rawUrl, emptyMap())

        assertEquals(rawUrl, result)
    }
}
