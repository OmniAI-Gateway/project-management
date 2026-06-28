package org.omniai.sdk.binders

import org.omniai.sdk.common.AttributeKey

class DslBinderBuilder {
    private val bindings = mutableListOf<BindingSpec<*>>()

    infix fun String.mappedTo(domainKey: AttributeKey<String>): Pair<String, AttributeKey<String>> = this to domainKey

    infix fun Pair<String, AttributeKey<String>>.takeFrom(source: Source) {
        bindings.add(BindingSpec(source, this.first, this.second) { it })
    }

    fun <T : Any> String.mappedTo(
        domainKey: AttributeKey<T>,
        decode: (String) -> T?,
    ): TypedPair<T> = TypedPair(this, domainKey, decode)

    infix fun <T : Any> TypedPair<T>.takeFrom(source: Source) {
        bindings.add(BindingSpec(source, externalKey, domainKey, decode))
    }

    fun build(): ConfigurableMetadataBinder = ConfigurableMetadataBinder(bindings.toList())

    data class TypedPair<T : Any>(
        val externalKey: String,
        val domainKey: AttributeKey<T>,
        val decode: (String) -> T?,
    )
}

fun buildDslMetadataBinder(block: DslBinderBuilder.() -> Unit): ConfigurableMetadataBinder = DslBinderBuilder().apply(block).build()
