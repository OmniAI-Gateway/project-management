package org.omniai.sdk.auth.interfaces

import org.omniai.sdk.auth.domain.Kid
import org.omniai.sdk.auth.domain.PublicKey

interface PublicKeyCache {
    fun get(kid: Kid): PublicKey?

    fun put(
        kid: Kid,
        publicKey: PublicKey,
    )
}
