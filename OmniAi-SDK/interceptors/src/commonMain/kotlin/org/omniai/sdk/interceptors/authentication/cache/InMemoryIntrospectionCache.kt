package org.omniai.sdk.interceptors.auth.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.omniai.sdk.interceptors.auth.domain.IntrospectionResult
import org.omniai.sdk.interceptors.auth.interfaces.IntrospectionCache
import kotlin.time.Duration
import kotlin.time.TimeSource

class InMemoryIntrospectionCache(
    positiveCacheTtl: Duration,
    negativeCacheTtl: Duration,
    private val maxCapacity: Int = 1000,
) : IntrospectionCache(positiveCacheTtl, negativeCacheTtl) {
    private data class CacheEntry(
        val result: IntrospectionResult,
        val expiresAt: TimeSource.Monotonic.ValueTimeMark,
    )

    private val cache = mutableMapOf<String, CacheEntry>()
    private val mutex = Mutex()

    override suspend fun store(
        hashedKey: String,
        result: IntrospectionResult,
        ttl: Duration,
    ) {
        mutex.withLock {
            if (cache.size >= maxCapacity && !cache.containsKey(hashedKey)) {
                cleanupExpired()
                if (cache.size >= maxCapacity) {
                    cache.keys.firstOrNull()?.let { cache.remove(it) }
                }
            }
            cache[hashedKey] =
                CacheEntry(
                    result = result,
                    expiresAt = TimeSource.Monotonic.markNow() + ttl,
                )
        }
    }

    override suspend fun retrieve(hashedKey: String): IntrospectionResult? {
        return mutex.withLock {
            val entry = cache[hashedKey] ?: return@withLock null
            if (entry.expiresAt.hasPassedNow()) {
                cache.remove(hashedKey)
                null
            } else {
                entry.result
            }
        }
    }

    override suspend fun delete(hashedKey: String) {
        mutex.withLock {
            cache.remove(hashedKey)
        }
    }

    private fun cleanupExpired() {
        val iterator = cache.entries.iterator()
        while (iterator.hasNext()) {
            if (iterator
                    .next()
                    .value.expiresAt
                    .hasPassedNow()
            ) {
                iterator.remove()
            }
        }
    }
}
