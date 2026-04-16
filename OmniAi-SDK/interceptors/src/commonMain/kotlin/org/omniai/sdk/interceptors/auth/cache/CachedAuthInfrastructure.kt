package org.omniai.sdk.interceptors.auth.cache

import org.omniai.sdk.interceptors.auth.AuthSecurityInfrastructure
import org.omniai.sdk.interceptors.auth.HttpAuthSecurityClient

class CachedAuthInfrastructure(
    private val networkClient: HttpAuthSecurityClient,
    private val cache: CachedPublicKeysProvider
) : AuthSecurityInfrastructure {

    override suspend fun getPublicKey(issuer: String, keyId: String?): Any {
        return cache.getPublicKey(issuer, keyId)
    }

    override suspend fun exchangeApiKey(apiKey: String): String {
        return networkClient.exchangeApiKey(apiKey)
    }
}