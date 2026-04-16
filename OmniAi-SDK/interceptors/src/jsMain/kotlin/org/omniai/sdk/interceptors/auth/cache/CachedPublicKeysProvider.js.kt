package org.omniai.sdk.interceptors.auth.cache

actual fun <K, V> createConcurrentMap(): MutableMap<K, V> = mutableMapOf<K, V>()
actual fun currentTimeMillis(): Long = kotlin.js.Date.now().toLong()