package org.omniai.sdk.interceptors.ratelimiting.domain

/**
 * The outcome of a rate limit evaluation by the RateLimitStore.
 */
sealed interface RateLimitResult {
    /**
     * Indicates that the request is within the allowed limits.
     *
     * @property remaining The number of remaining units (requests, tokens) in the current window.
     */
    data class Allowed(
        val remaining: Int,
    ) : RateLimitResult

    /**
     * Indicates that the request has exceeded the allowed limits.
     *
     * @property retryAfter The amount of time the client should wait before retrying.
     */
    data class Throttled(
        val retryAfter: kotlin.time.Duration,
    ) : RateLimitResult
}
