package org.omniai.sdk.interceptors.ratelimiting.store

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import org.omniai.sdk.interceptors.ratelimiting.domain.RateLimitResult
import org.omniai.sdk.interceptors.ratelimiting.domain.RateLimitTarget
import kotlin.time.Duration.Companion.seconds

/**
 * A simple, in-memory implementation of [RateLimitStore].
 *
 * Uses coroutine Mutexes for safe concurrent access.
 * This is primarily useful for single-instance applications or testing.
 * In a distributed Gateway deployment, a Redis-backed store should be used.
 */
class InMemoryRateLimitStore : RateLimitStore {
    private data class BucketState(
        var tokens: Int,
        var windowStartTimeEpochSeconds: Long,
    )

    private val buckets = mutableMapOf<String, BucketState>()
    private val mutex = Mutex()

    override suspend fun consume(
        target: RateLimitTarget,
        weight: Int,
    ): RateLimitResult {
        val now = Clock.System.now().epochSeconds
        val windowSeconds = target.definition.window.duration.inWholeSeconds

        return mutex.withLock {
            val state =
                buckets.getOrPut(target.key) {
                    BucketState(target.definition.limit, now)
                }

            // Check if the current window has expired; if so, reset the bucket.
            if (now - state.windowStartTimeEpochSeconds >= windowSeconds) {
                state.tokens = target.definition.limit
                state.windowStartTimeEpochSeconds = now
            }

            if (state.tokens >= weight) {
                state.tokens -= weight
                RateLimitResult.Allowed(state.tokens)
            } else {
                val retryAfter = windowSeconds - (now - state.windowStartTimeEpochSeconds)
                RateLimitResult.Throttled(retryAfter = retryAfter.coerceAtLeast(0).seconds)
            }
        }
    }
}
