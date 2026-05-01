package org.omniai.sdk.interceptors.ratelimiting.domain

/**
 * A concrete target (consisting of an identifier key and a rule set) 
 * to be tracked by the RateLimitStore.
 * 
 * @property key The unique identifier for this target (e.g., "user:123:requests").
 * @property definition The rate limit constraints applied to this target.
 */
data class RateLimitTarget(
    val key: String,
    val definition: RateLimitDefinition
)
