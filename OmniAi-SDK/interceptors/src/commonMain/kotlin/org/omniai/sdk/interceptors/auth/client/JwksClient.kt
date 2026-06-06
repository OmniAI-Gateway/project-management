package org.omniai.sdk.interceptors.auth.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.omniai.sdk.interceptors.auth.domain.HttpAuthSecurityClientConfig
import org.omniai.sdk.interceptors.auth.domain.Kid
import org.omniai.sdk.interceptors.auth.domain.PublicKey
import org.omniai.sdk.interceptors.auth.dto.JwksDto
import org.omniai.sdk.interceptors.auth.interfaces.PublicKeyCache
import org.omniai.sdk.ports.outbound.http.*
import kotlin.concurrent.Volatile
import kotlin.time.TimeSource

class JwksClient(
    private val config: HttpAuthSecurityClientConfig,
    private val publicKeyCache: PublicKeyCache,
    private val httpClient: HttpTransportClient,
    private val jwksUri: String,
) {

    @Volatile
    private var lastFetchEpochNanos: Long = -config.minimumTimeToFetchKeys.inWholeNanoseconds

    private val startMark = TimeSource.Monotonic.markNow()

    private val requestJwks by lazy { requestConfig<Unit>(jwksUri) { method = HttpMethod.GET } }

    init {
        startBackgroundKeyRotation()
    }

    private fun startBackgroundKeyRotation() {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                delay(config.backgroundJwksRefreshInterval)
                if (cooldownElapsed()) {
                    fetchAndUpdateKeys()
                }
            }
        }
    }

    suspend fun getPublicKey(keyId: Kid): PublicKey? {
        publicKeyCache.get(keyId)?.let { return it }
        if (cooldownElapsed()) {
            fetchAndUpdateKeys()
        }

        return publicKeyCache.get(keyId)
    }

    private fun cooldownElapsed(): Boolean {
        val elapsedNanos = startMark.elapsedNow().inWholeNanoseconds - lastFetchEpochNanos
        return elapsedNanos >= config.minimumTimeToFetchKeys.inWholeNanoseconds
    }

    private suspend fun fetchAndUpdateKeys() {
        when (val result = httpClient.executeRequest<JwksDto, Unit>(requestJwks)) {
            is HttpCallResult.Success -> {
                val keys = result.data.keys.mapNotNull { key -> key.toDomain() }
                keys.forEach { (kid, publicKey) -> publicKeyCache.put(kid, publicKey) }
            }
            else -> Unit
        }
        lastFetchEpochNanos = startMark.elapsedNow().inWholeNanoseconds
    }
}
