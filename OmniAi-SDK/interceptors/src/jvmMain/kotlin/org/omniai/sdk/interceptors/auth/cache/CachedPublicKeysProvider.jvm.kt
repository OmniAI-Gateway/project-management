package org.omniai.sdk.interceptors.auth.cache

import java.util.concurrent.ConcurrentHashMap

actual fun <K, V> createConcurrentMap(): MutableMap<K, V> = ConcurrentHashMap<K, V>()
actual fun currentTimeMillis(): Long = System.currentTimeMillis()

