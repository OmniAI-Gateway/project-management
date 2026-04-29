package org.omniai.sdk.auth.cache

import org.omniai.sdk.auth.domain.Kid
import org.omniai.sdk.auth.domain.PublicKey
import org.omniai.sdk.auth.interfaces.PublicKeyCache

expect class ConcurrentCacheMap<K, V>() {
    fun get(key: K): V?
    fun put(key: K, value: V)
    fun clear()
}

class InMemoryKeyCache : PublicKeyCache {

    private val cache = ConcurrentCacheMap<Kid, PublicKey>()

    override fun get(kid: Kid): PublicKey? {
        return cache.get(kid)
    }

    override fun put(kid: Kid, publicKey: PublicKey) {
        cache.put(kid, publicKey)
    }

    fun clearCache() {
        cache.clear()
    }
}