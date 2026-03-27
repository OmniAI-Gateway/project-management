package org.omniai.sdk.core.commom

import kotlin.reflect.KClass

class AttributeKey<T : Any> @PublishedApi internal constructor(
    val name: String,
    val type: KClass<T>
) {
    override fun equals(other: Any?): Boolean =
        (other is AttributeKey<*>) && name == other.name && type == other.type

    override fun hashCode(): Int = name.hashCode() * 31 + type.hashCode()

    override fun toString(): String = "Key($name: ${type.simpleName})"
}

inline fun <reified T : Any> key(name: String) =
    AttributeKey(name, T::class)