package org.omniai.sdk.interceptors.auth.interfaces

import org.omniai.sdk.interceptors.auth.domain.IntrospectionResult
import org.omniai.sdk.interceptors.auth.domain.OPAQUE
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * Abstract base class for caching OAuth 2.0 Token Introspection results (RFC 7662).
 *
 * This cache automatically computes the safest Time-To-Live (TTL) for each token.
 * For valid tokens, it chooses the minimum between the configured TTL and the token's actual `exp`.
 * For invalid tokens, it uses a short, configured negative TTL to prevent brute-force attacks.
 *
 * It also ensures that raw opaque tokens are never exposed to the underlying
 * storage mechanism by hashing them prior to storage.
 */
abstract class IntrospectionCache(
    private val positiveCacheTtl: Duration,
    private val negativeCacheTtl: Duration
) {

    /**
     * Stores the introspection result in the underlying storage.
     *
     * @param hashedKey The secure hash of the opaque token.
     * @param result The introspection result (can be active or inactive).
     * @param ttl The strictly calculated Time-To-Live duration for this specific entry.
     */
    protected abstract suspend fun store(hashedKey: String, result: IntrospectionResult, ttl: Duration)

    /**
     * Retrieves the introspection result from the underlying storage, if it exists and is not expired.
     *
     * @param hashedKey The secure hash of the opaque token.
     * @return The cached [IntrospectionResult], or null if missing/expired.
     */
    protected abstract suspend fun retrieve(hashedKey: String): IntrospectionResult?

    /**
     * Removes the token entry from the underlying storage.
     *
     * @param hashedKey The secure hash of the opaque token.
     */
    protected abstract suspend fun delete(hashedKey: String)

    /**
     * Generates a secure hash for the raw token.
     *
     * IMPORTANT KMP NOTE: Since Kotlin Standard Library does not provide built-in cryptographic
     * hashing, you must replace this placeholder with an actual KMP crypto library implementation
     * (e.g., Okio's ByteString.encodeUtf8().sha256().hex() or Krypto).
     * Do NOT use standard `hashCode()` in production environments.
     *
     * @param rawToken The plain text opaque token.
     * @return A secure hashed string representation of the token.
     */
    protected open fun hashToken(rawToken: String): String {
        // TODO: Replace with an actual KMP SHA-256 implementation
        return rawToken.hashCode().toString()
    }

    /**
     * Caches an introspection result. The TTL is automatically calculated based on the
     * cache configuration and the token's expiration claim (`exp`).
     *
     * @param token The opaque token wrapper.
     * @param result The introspection result to cache.
     */
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

    /**
     * Retrieves a cached introspection result.
     *
     * @param token The opaque token wrapper.
     * @return The cached result, or null if it's expired or not present.
     */
    suspend fun get(token: OPAQUE): IntrospectionResult? {
        val hashedKey = hashToken(token.token.token)
        return retrieve(hashedKey)
    }

    /**
     * Evicts a token from the cache.
     *
     * @param token The opaque token wrapper.
     */
    suspend fun remove(token: OPAQUE) {
        val hashedKey = hashToken(token.token.token)
        delete(hashedKey)
    }
}
