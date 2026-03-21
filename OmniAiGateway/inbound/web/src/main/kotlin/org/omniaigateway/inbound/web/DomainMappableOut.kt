package org.omniaigateway.inbound.web

/**
 * Common contract for mappers that convert domain objects to transport/output models.
 */
interface DomainMappableOut<in D, out O> {
    fun fromDomain(domain: D): O
}

