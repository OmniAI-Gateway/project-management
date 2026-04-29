package org.omniai.sdk.interceptors.auth

interface AuthSecurityInfrastructure : PublicKeysProvider, TokenIntrospector

interface PublicKeysProvider {
    suspend fun getPublicKey(issuer: String, keyId: String?): Any
}

interface TokenIntrospector {
    suspend fun introspectToken(token: String): Map<String, Any>
}
