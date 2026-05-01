package org.omniai.sdk.interceptors.auth.interfaces

import org.omniai.sdk.interceptors.auth.domain.Kid
import org.omniai.sdk.interceptors.auth.domain.PublicKey

interface PublicKeyCache {
    fun get(kid: Kid): PublicKey?

    fun put(
        kid: Kid,
        publicKey: PublicKey,
    )
}
