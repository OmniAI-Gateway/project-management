package org.omniai.sdk.core.http

import io.ktor.http.HttpStatusCode
import org.omniai.sdk.domain.errors.ErrorCode

object ErrorHttpMapper {
    private val codeMap = mapOf(
        ErrorCode.BAD_REQUEST to HttpStatusCode.BadRequest,
        ErrorCode.UNAUTHORIZED to HttpStatusCode.Unauthorized,
        ErrorCode.FORBIDDEN to HttpStatusCode.Forbidden,
        ErrorCode.NOT_FOUND to HttpStatusCode.NotFound,
        ErrorCode.CONFLICT to HttpStatusCode.Conflict,
        ErrorCode.TOO_MANY_REQUESTS to HttpStatusCode.TooManyRequests,
        ErrorCode.INTERNAL_SERVER_ERROR to HttpStatusCode.InternalServerError,
        ErrorCode.PROVIDER_ERROR to HttpStatusCode.BadGateway,
        ErrorCode.SERVICE_UNAVAILABLE to HttpStatusCode.ServiceUnavailable,
        ErrorCode.TIMEOUT to HttpStatusCode.RequestTimeout,
        ErrorCode.GATEWAY_TIMEOUT to HttpStatusCode.GatewayTimeout,
        ErrorCode.UNKNOWN_ERROR to HttpStatusCode.InternalServerError
    )

    fun toHttpStatusCode(code: ErrorCode): HttpStatusCode {
        return codeMap[code] ?: HttpStatusCode.InternalServerError
    }
}
