package org.omniai.sdk.interceptors.ratelimiting.domain

/**
 * Represents the type of rate limit being enforced.
 */
enum class RateLimitType {
    REQUESTS,
    TOKENS
}
