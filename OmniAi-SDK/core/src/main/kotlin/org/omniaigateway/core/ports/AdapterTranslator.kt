package org.omniaigateway.core.ports

/**
 * Converts core domain models to provider-facing contracts.
 */
interface AdapterTranslator<in D, out O> {
    fun fromDomain(domain: D): O
}

