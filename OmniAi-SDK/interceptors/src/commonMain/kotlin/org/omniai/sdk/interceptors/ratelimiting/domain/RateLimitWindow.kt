package org.omniai.sdk.interceptors.ratelimiting.domain

import kotlin.time.Duration

/**
 * Represents the time window for a rate limit.
 * 
 * @property duration The specific duration of the window (e.g., 1.minutes, 1.hours).
 */
data class RateLimitWindow(
    val duration: Duration
)
