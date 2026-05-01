package org.omniai.sdk.interceptors.ratelimiting.store

import org.omniai.sdk.interceptors.ratelimiting.domain.RateLimitResult
import org.omniai.sdk.interceptors.ratelimiting.domain.RateLimitTarget

/**
 * Service Provider Interface (SPI) for rate limit storage mechanisms.
 * 
 * Implementations of this interface (e.g., InMemory, Redis) are responsible
 * for tracking the consumption of limits and determining if a request should
 * be allowed or throttled based on the [RateLimitTarget].
 */
interface RateLimitStore {
    
    /**
     * Consumes units from the specified rate limit target.
     * 
     * @param target The target definition including the key, window, and limit.
     * @param weight The amount to consume from the bucket (e.g., 1 for a request, N for tokens). Defaults to 1.
     * @return A [RateLimitResult] indicating whether the consumption was allowed or throttled.
     */
    suspend fun consume(target: RateLimitTarget, weight: Int = 1): RateLimitResult
}
