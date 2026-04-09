package org.omniai.sdk.core.commom

import kotlin.reflect.KClass


class TypedMap(private val data: MutableMap<AttributeKey<*>, Any> = mutableMapOf()) {

    companion object  {

        private val cache = mutableMapOf<Pair<String, KClass<*>>, AttributeKey<*>>()

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> get(name: String, type: KClass<T>): AttributeKey<T> {
            return cache.getOrPut(name to type) { AttributeKey(name, type) } as AttributeKey<T>
        }
    }

    fun <T : Any> put(key: AttributeKey<T>, value: T) {
        data[key] = value
    }

    operator fun <T : Any> set(key: AttributeKey<T>, value: T) = put(key, value)

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(key: AttributeKey<T>): T? = data[key] as? T

    fun <T : Any> require(key: AttributeKey<T>): T =
        get(key) ?: error("Missing required key: $key")

    fun contains(key: AttributeKey<*>): Boolean = key in data

    fun remove(key: AttributeKey<*>) = data.remove(key)

    inline fun <T : Any> getOrPut(key: AttributeKey<T>, default: () -> T): T =
        get(key) ?: default().also { put(key, it) }

    inline fun <reified T : Any> put(name: String, value: T) {
        val key = get(name, T::class)
        put(key, value)
    }

    inline operator fun <reified T : Any> get(name: String): T? {
        val key = get(name, T::class)
        return get(key)
    }

    inline fun <reified T : Any> require(name: String): T {
        val key = get(name, T::class)
        return require(key)
    }

    inline fun <reified T : Any> contains(name: String): Boolean {
        val key = get(name, T::class)
        return contains(key)
    }

    inline fun <reified T : Any> remove(name: String) {
        val key = get(name, T::class)
        remove(key)
    }

    inline fun <reified T : Any> getOrPut(name: String, noinline default: () -> T): T {
        val key = get(name, T::class)
        return getOrPut(key, default)
    }

    fun putAll(other: TypedMap) = data.putAll(other.data)

    fun copy(): TypedMap = TypedMap(data.toMutableMap())

    fun clear() = data.clear()

    fun keys(): Set<AttributeKey<*>> = data.keys

    fun isEmpty(): Boolean = data.isEmpty()

    fun size(): Int = data.size

    override fun toString(): String =
        data.entries.joinToString(prefix = "TypedMap(", postfix = ")") { "${it.key}=${it.value}" }
}