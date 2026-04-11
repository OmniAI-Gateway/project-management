package org.omniai.sdk.binders

import org.omniai.sdk.core.commom.AttributeKey
import org.omniai.sdk.core.commom.TypedMap

class ConfigurableMetadataBinder(
    private val bindings: List<BindingSpec<*>> = emptyList()
) {
    fun bind(context: IncomingContext): TypedMap {
        val typedMap = TypedMap()

        bindings.forEach { binding ->
            val rawValue = context.getRaw(binding.source, binding.externalKey) ?: return@forEach
            binding.writeIfParsed(rawValue, typedMap)
        }

        return typedMap
    }
}

data class BindingSpec<T : Any>(
    val source: Source,
    val externalKey: String,
    val domainKey: AttributeKey<T>,
    val decode: (String) -> T?
) {
    fun writeIfParsed(rawValue: String, target: TypedMap) {
        decode(rawValue)?.let { decoded -> target.put(domainKey, decoded) }
    }
}
