package org.omniai.sdk.auth.interfaces

import org.omniai.sdk.auth.domain.Kid
import org.omniai.sdk.auth.domain.PublicKey

interface PublicKeysProvider {
    suspend fun getPublicKey(keyId: Kid): PublicKey?
}

