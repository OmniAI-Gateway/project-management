package org.omniai.sdk.interceptors.ratelimiting.domain

/**
 * Defines the threshold and constraints for a rate limit.
 *
 * @property limit The maximum number of units allowed within the window.
 * @property type The type of limit being enforced (e.g., REQUESTS, TOKENS).
 * @property window The time window during which the limit applies.
 */
data class RateLimitDefinition(
    val limit: Int,
    val type: RateLimitType,
    val window: RateLimitWindow,
)
