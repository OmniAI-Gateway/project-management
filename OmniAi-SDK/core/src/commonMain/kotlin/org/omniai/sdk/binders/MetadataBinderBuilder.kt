package org.omniai.sdk.binders

import org.omniai.sdk.common.AttributeKey

class MetadataBinderBuilder {
    private val bindings = mutableListOf<BindingSpec<*>>()

    abstract inner class BaseBindingContext(
        private val source: Source,
        private val key: String,
    ) {
        infix fun bindTo(domainKey: AttributeKey<String>) {
            addBinding(domainKey) { it }
        }

        fun <T : Any> bindTo(
            domainKey: AttributeKey<T>,
            decode: (String) -> T?,
        ) {
            addBinding(domainKey, decode)
        }

        fun bindToInt(domainKey: AttributeKey<Int>) {
            addBinding(domainKey) { it.toIntOrNull() }
        }

        fun bindToLong(domainKey: AttributeKey<Long>) {
            addBinding(domainKey) { it.toLongOrNull() }
        }

        fun bindToBoolean(domainKey: AttributeKey<Boolean>) {
            addBinding(domainKey) { it.toBooleanStrictOrNull() }
        }

        private fun <T : Any> addBinding(
            domainKey: AttributeKey<T>,
            decode: (String) -> T?,
        ) {
            bindings.add(BindingSpec(source, key, domainKey, decode))
        }
    }

    inner class HeaderContext(
        key: String,
    ) : BaseBindingContext(Source.HEADER, key)

    inner class QueryContext(
        key: String,
    ) : BaseBindingContext(Source.QUERY, key)

    inner class PathContext(
        key: String,
    ) : BaseBindingContext(Source.PATH, key)

    inner class PropertyContext(
        key: String,
    ) : BaseBindingContext(Source.PROPERTY, key)

    fun header(key: String) = HeaderContext(key)

    fun query(key: String) = QueryContext(key)

    fun path(key: String) = PathContext(key)

    fun property(key: String) = PropertyContext(key)

    fun build(): ConfigurableMetadataBinder = ConfigurableMetadataBinder(bindings.toList())
}

fun buildMetadataBinder(block: MetadataBinderBuilder.() -> Unit): ConfigurableMetadataBinder = MetadataBinderBuilder().apply(block).build()
