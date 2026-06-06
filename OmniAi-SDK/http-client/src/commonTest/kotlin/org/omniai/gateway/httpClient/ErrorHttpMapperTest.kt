package org.omniai.gateway.httpClient

import org.omniai.sdk.core.http.ErrorHttpMapper
import io.ktor.http.HttpStatusCode
import org.omniai.sdk.domain.errors.ErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ErrorHttpMapperTest {

    @Test
    fun `BAD_REQUEST maps to 400`() {
        assertEquals(HttpStatusCode.BadRequest, ErrorHttpMapper.toHttpStatusCode(ErrorCode.BAD_REQUEST))
    }

    @Test
    fun `UNAUTHORIZED maps to 401`() {
        assertEquals(HttpStatusCode.Unauthorized, ErrorHttpMapper.toHttpStatusCode(ErrorCode.UNAUTHORIZED))
    }

    @Test
    fun `FORBIDDEN maps to 403`() {
        assertEquals(HttpStatusCode.Forbidden, ErrorHttpMapper.toHttpStatusCode(ErrorCode.FORBIDDEN))
    }

    @Test
    fun `NOT_FOUND maps to 404`() {
        assertEquals(HttpStatusCode.NotFound, ErrorHttpMapper.toHttpStatusCode(ErrorCode.NOT_FOUND))
    }

    @Test
    fun `CONFLICT maps to 409`() {
        assertEquals(HttpStatusCode.Conflict, ErrorHttpMapper.toHttpStatusCode(ErrorCode.CONFLICT))
    }

    @Test
    fun `TOO_MANY_REQUESTS maps to 429`() {
        assertEquals(HttpStatusCode.TooManyRequests, ErrorHttpMapper.toHttpStatusCode(ErrorCode.TOO_MANY_REQUESTS))
    }

    @Test
    fun `INTERNAL_SERVER_ERROR maps to 500`() {
        assertEquals(
            HttpStatusCode.InternalServerError,
            ErrorHttpMapper.toHttpStatusCode(ErrorCode.INTERNAL_SERVER_ERROR)
        )
    }

    @Test
    fun `PROVIDER_ERROR maps to 502`() {
        assertEquals(HttpStatusCode.BadGateway, ErrorHttpMapper.toHttpStatusCode(ErrorCode.PROVIDER_ERROR))
    }

    @Test
    fun `SERVICE_UNAVAILABLE maps to 503`() {
        assertEquals(
            HttpStatusCode.ServiceUnavailable,
            ErrorHttpMapper.toHttpStatusCode(ErrorCode.SERVICE_UNAVAILABLE)
        )
    }

    @Test
    fun `TIMEOUT maps to 408`() {
        assertEquals(HttpStatusCode.RequestTimeout, ErrorHttpMapper.toHttpStatusCode(ErrorCode.TIMEOUT))
    }

    @Test
    fun `GATEWAY_TIMEOUT maps to 504`() {
        assertEquals(HttpStatusCode.GatewayTimeout, ErrorHttpMapper.toHttpStatusCode(ErrorCode.GATEWAY_TIMEOUT))
    }

    @Test
    fun `UNKNOWN_ERROR maps to 500`() {
        assertEquals(
            HttpStatusCode.InternalServerError,
            ErrorHttpMapper.toHttpStatusCode(ErrorCode.UNKNOWN_ERROR)
        )
    }

    @Test
    fun `all ErrorCode values are explicitly mapped and not falling through to default 500`() {
        val legitimately500 = setOf(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.UNKNOWN_ERROR)

        ErrorCode.entries.forEach { code ->
            val status = ErrorHttpMapper.toHttpStatusCode(code)
            if (code !in legitimately500) {
                assertNotEquals(
                    HttpStatusCode.InternalServerError,
                    status,
                    "ErrorCode.$code unexpectedly resolved to 500 — it may be missing from the codeMap"
                )
            }
        }
    }

    @Test
    fun `toHttpStatusCode always returns a non-null HttpStatusCode`() {
        ErrorCode.entries.forEach { code ->
            val status = ErrorHttpMapper.toHttpStatusCode(code)
            assertTrue(
                status.value in 100..599,
                "Unexpected status value ${status.value} for ErrorCode.$code"
            )
        }
    }
}
