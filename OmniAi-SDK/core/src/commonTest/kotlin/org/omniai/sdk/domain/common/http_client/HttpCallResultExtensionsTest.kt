package org.omniai.sdk.domain.common.http_client

import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.errors.ApiDownError
import org.omniai.sdk.domain.errors.ParsingError
import org.omniai.sdk.domain.errors.TooManyRequestsError
import org.omniai.sdk.domain.errors.UnauthorizedError
import org.omniai.sdk.ports.outbound.http.HttpCallResult
import org.omniai.sdk.ports.outbound.http.toDomainError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpCallResultExtensionsTest {
    // Simple Provider mock for testing
    private val mockProvider = Provider("MockProvider")

    @Test
    fun `should map 429 error to TooManyRequestsError`() {
        val apiError = HttpCallResult.ApiError(code = 429, message = "Rate limit exceeded")

        val domainError = apiError.toDomainError(mockProvider)

        assertTrue(domainError is TooManyRequestsError)
        assertEquals("MockProvider rate limit exceeded: Rate limit exceeded", domainError.message)
    }

    @Test
    fun `should map 401 error to UnauthorizedError`() {
        val apiError = HttpCallResult.ApiError(code = 401, message = "Invalid API Key")

        val domainError = apiError.toDomainError(mockProvider)

        assertTrue(domainError is UnauthorizedError)
        assertEquals("MockProvider rejected request with status 401: Invalid API Key", domainError.message)
    }

    @Test
    fun `should map 500 error to ApiDownError`() {
        val apiError = HttpCallResult.ApiError(code = 503, message = "Service Unavailable")

        val domainError = apiError.toDomainError(mockProvider)

        assertTrue(domainError is ApiDownError)
        assertEquals("MockProvider API is unavailable (status 503)", domainError.message)
    }

    @Test
    fun `should map NetworkError to ApiDownError with original exception`() {
        val exception = RuntimeException("Connection reset")
        val networkError = HttpCallResult.NetworkError(exception)

        val domainError = networkError.toDomainError(mockProvider)

        assertTrue(domainError is ApiDownError)
        assertEquals("MockProvider API request failed due to network issues", domainError.message)
        assertEquals(exception, domainError.cause)
    }

    @Test
    fun `should map SerializationError to ParsingError`() {
        val exception = RuntimeException("Unexpected JSON token")
        val serializationError = HttpCallResult.SerializationError(exception)

        val domainError = serializationError.toDomainError(mockProvider)

        assertTrue(domainError is ParsingError)
        assertEquals("Failed to parse MockProvider API response", domainError.message)
    }
}
