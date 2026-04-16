package org.omniai.sdk.interceptors.auth

interface AuthSecurityInfrastructure : PublicKeysProvider, TokenExchanger

interface PublicKeysProvider {
    suspend fun getPublicKey(issuer: String, keyId: String?): Any
}

interface TokenExchanger {
    suspend fun exchangeApiKey(apiKey: String): String
}

