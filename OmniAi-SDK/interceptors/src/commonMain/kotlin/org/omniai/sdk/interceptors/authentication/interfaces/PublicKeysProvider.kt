package org.omniai.sdk.interceptors.auth.interfaces

import org.omniai.sdk.interceptors.auth.domain.Kid
import org.omniai.sdk.interceptors.auth.domain.PublicKey

interface PublicKeysProvider {
    suspend fun getPublicKey(keyId: Kid): PublicKey?
}
