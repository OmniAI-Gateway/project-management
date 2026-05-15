package org.omniai.sdk.domain.errors

/**
 * Domain error representing a rate limiting or quota violation.
 * Typically maps to an HTTP 429 Too Many Requests response.
 */
data class TooManyRequestsError(
    override val message: String,
    val retryAfter: kotlin.time.Duration? = null,
    override val cause: Throwable? = null,
    override val code: ErrorCode = ErrorCode.TOO_MANY_REQUESTS
) : DomainError
