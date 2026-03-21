package org.omniaigateway.inbound.web

/**
 * Common contract for transport-layer models that can be converted to domain models.
 */
interface DomainMappableIn<T> {
    fun toDomain(): T
}