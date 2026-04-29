package org.omniai.sdk.auth

import org.omniai.sdk.auth.domain.HttpAuthSecurityClientConfig
import org.omniai.sdk.auth.domain.IntrospectionResult
import org.omniai.sdk.auth.domain.Kid
import org.omniai.sdk.auth.domain.PublicKey
import org.omniai.sdk.auth.dto.IntrospectionDto
import org.omniai.sdk.auth.dto.JwksDto
import org.omniai.sdk.core.http.*
import org.omniai.sdk.auth.interfaces.AuthSecurityInfrastructure
import org.omniai.sdk.auth.interfaces.PublicKeyCache
import kotlin.io.encoding.Base64

class HttpAuthSecurityClient(
    private val config: HttpAuthSecurityClientConfig,
    private val publicKeyCache: PublicKeyCache,
    private val httpClient: HttpTransportClient,
    private val jwksUri: String,
    private val introspectionEndpoint: String?,
) : AuthSecurityInfrastructure {

    override suspend fun getPublicKey(keyId: Kid): PublicKey? {
        publicKeyCache.get(keyId)?.let { return it }

        val config = requestConfig<Unit>(jwksUri) { method = HttpMethod.GET }

        return when (val result = httpClient.executeRequest<JwksDto, Unit>(config)) {
            is HttpCallResult.Success -> {
                val data = result.data.keys.mapNotNull{ it.toDomain() }
                data.forEach {
                    publicKeyCache.put(it.first, it.second)
                }
                return data.find { it.first == keyId }?.second
            }
            else -> null
        }
    }

    override suspend fun introspectToken(token: String): IntrospectionResult? {
        val url = introspectionEndpoint ?: throw IllegalStateException("Introspection endpoint não configurado")

        val config = requestConfig(url) {
            method = HttpMethod.POST
            header("Content-Type", "application/x-www-form-urlencoded")
            val credentials = "${config.authClientId}:${config.authClientSecret}"
            val encodedCredentials = Base64.encode(credentials.encodeToByteArray())
            header("Authorization", "Basic $encodedCredentials")
            
            body = "token=${urlEncode(token)}"
        }
        return when (val result = httpClient.executeRequest<IntrospectionDto, String>(config)) {
            is HttpCallResult.Success -> {
                val dto = result.data
                if (dto.active) {
                    dto.toDomain()
                } else {
                    null
                }
            }
            else -> null
        }
    }
}