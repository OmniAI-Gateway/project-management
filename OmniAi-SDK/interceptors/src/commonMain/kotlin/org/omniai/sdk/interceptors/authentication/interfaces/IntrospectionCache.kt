package org.omniai.sdk.interceptors.auth.interfaces

import org.omniai.sdk.interceptors.auth.domain.IntrospectionResult
import org.omniai.sdk.interceptors.auth.domain.OPAQUE
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

abstract class IntrospectionCache(
    private val positiveCacheTtl: Duration,
    private val negativeCacheTtl: Duration
) {
    protected abstract suspend fun store(hashedKey: String, result: IntrospectionResult, ttl: Duration)
    protected abstract suspend fun retrieve(hashedKey: String): IntrospectionResult?
    protected abstract suspend fun delete(hashedKey: String)
    protected open fun hashToken(rawToken: String): String {
        // TODO: Replace with an actual KMP SHA-256 implementation
        return rawToken.hashCode().toString()
    }

    suspend fun put(token: OPAQUE, result: IntrospectionResult) {
        val hashedKey = hashToken(token.token.token)

        val finalTtl = if (result.active) {
            if (result.exp != null) {
                val nowSeconds = Clock.System.now().epochSeconds
                val remainingSeconds = result.exp - nowSeconds

                if (remainingSeconds <= 0) {
                    return
                }
                minOf(positiveCacheTtl, remainingSeconds.seconds)
            } else {
                positiveCacheTtl
            }
        } else {
            negativeCacheTtl
        }

        store(hashedKey, result, finalTtl)
    }

    suspend fun get(token: OPAQUE): IntrospectionResult? {
        val hashedKey = hashToken(token.token.token)
        return retrieve(hashedKey)
    }

    suspend fun remove(token: OPAQUE) {
        val hashedKey = hashToken(token.token.token)
        delete(hashedKey)
    }
}

