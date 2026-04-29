package org.omniai.sdk.auth.cache


actual class ConcurrentCacheMap<K, V> actual constructor() {

    private val map = HashMap<K, V>()

    actual fun get(key: K): V? = map[key]

    actual fun put(key: K, value: V) {
        map[key] = value
    }

    actual fun clear() {
        map.clear()
    }
}