package org.omniai.sdk.interceptors.auth.cache

expect class CacheMap<K, V>() {
    fun get(key: K): V?
    fun put(key: K, value: V)
}

class PublicKeyCache {
    private val storage = CacheMap<String, Any>()

    fun get(kid: String): Any? = storage.get(kid)
    fun put(kid: String, key: Any) = storage.put(kid, key)
}