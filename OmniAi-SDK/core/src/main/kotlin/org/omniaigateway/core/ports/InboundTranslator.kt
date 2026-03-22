package org.omniaigateway.core.ports

import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.requests.CommonRequest

/**
 * Translates provider-specific inbound payloads into the core domain request model.
 */
interface InboundTranslator<in T> {
    val provider: Provider

    fun supports(providerId: String): Boolean = provider.name.equals(providerId, ignoreCase = true)

    fun toDomain(payload: T): CommonRequest
}

