package org.omniai.sdk.interceptors.auth.cache

import java.util.concurrent.ConcurrentHashMap

actual class CacheMap<K, V> {
    private val delegate = ConcurrentHashMap<K, V>()
    actual fun get(key: K): V? = delegate[key]
    actual fun put(key: K, value: V) { delegate[key] = value }
}

