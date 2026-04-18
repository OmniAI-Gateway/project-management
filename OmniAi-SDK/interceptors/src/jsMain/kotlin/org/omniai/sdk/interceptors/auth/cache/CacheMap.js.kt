package org.omniai.sdk.interceptors.auth.cache

actual class CacheMap<K, V> actual constructor() {
    private val delegate = mutableMapOf<K, V>()
    actual fun get(key: K): V? {
        return delegate[key]
    }

    actual fun put(key: K, value: V) {
        delegate[key] = value
    }
}