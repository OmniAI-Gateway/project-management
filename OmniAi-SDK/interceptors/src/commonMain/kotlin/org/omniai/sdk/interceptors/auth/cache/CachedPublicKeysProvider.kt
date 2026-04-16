package org.omniai.sdk.interceptors.auth.cache

import org.omniai.sdk.interceptors.auth.PublicKeysProvider

expect fun <K, V> createConcurrentMap(): MutableMap<K, V>
expect fun currentTimeMillis(): Long

private data class CachedKey(
    val key: Any,
    val expiresAt: Long
)

class CachedPublicKeysProvider(
    private val delegate: PublicKeysProvider,
    private val ttlMillis: Long = 3600000
) : PublicKeysProvider {

    private val cache = createConcurrentMap<String, CachedKey>()

    override suspend fun getPublicKey(issuer: String, keyId: String?): Any {
        val cacheKey = "$issuer:$keyId"
        val now = currentTimeMillis()

        cache[cacheKey]?.takeIf { it.expiresAt > now }?.let { return it.key }

        val freshKey = delegate.getPublicKey(issuer, keyId)
        cache[cacheKey] = CachedKey(freshKey, now + ttlMillis)

        return freshKey
    }
}