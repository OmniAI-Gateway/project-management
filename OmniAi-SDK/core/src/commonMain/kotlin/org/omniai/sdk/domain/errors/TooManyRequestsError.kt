package org.omniai.sdk.domain.errors

data class TooManyRequestsError(
    override val message: String,
    val retryAfter: kotlin.time.Duration? = null,
    override val cause: Throwable? = null,
    override val code: ErrorCode = ErrorCode.TOO_MANY_REQUESTS
) : DomainError
