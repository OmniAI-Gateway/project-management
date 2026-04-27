package org.omniai.sdk.interceptors.auth

import org.omniai.sdk.core.http.*
import org.omniai.sdk.interceptors.auth.cache.PublicKeyCache
import org.omniai.sdk.interceptors.auth.domain.TokenExchangeResponse

expect fun parseAllKeysFromJson(jwksJson: String): Map<String, Any>

class HttpAuthSecurityClient(
    private val httpClient: HttpTransportClient,
    private val jwksUri: String,
    private val introspectionEndpoint: String?,
    private val configSource: ConfigSource
) : AuthSecurityInfrastructure {

    private val publicKeyCache = PublicKeyCache()

    override suspend fun getPublicKey(issuer: String, keyId: String?): Any {
        val cacheKey = keyId ?: issuer
        publicKeyCache.get(cacheKey)?.let { return it }

        //Se não estiver na cache fazemos o pedido ao AS
        val config = requestConfig<Unit>(jwksUri) { method = HttpMethod.GET }

        return when (val result = httpClient.executeRequest<String, Unit>(config)) {
            is HttpCallResult.Success -> {

                val allKeys = parseAllKeysFromJson(result.data)
                allKeys.forEach { (id, key) ->
                    publicKeyCache.put(id, key)
                }

                allKeys[keyId] ?: allKeys.values.firstOrNull() ?: throw RuntimeException("Chave $keyId não encontrada no AS")
            }
            is HttpCallResult.ApiError -> throw RuntimeException("Erro AS: ${result.code}")
            else -> throw RuntimeException("Erro de rede ao carregar chaves")
        }
    }

    override suspend fun introspectToken(token: String): Map<String, Any> {
        val url = introspectionEndpoint ?: throw IllegalStateException("Introspection endpoint não configurado")

        val config = requestConfig(url) {
            method = HttpMethod.POST
            header("Content-Type", "application/x-www-form-urlencoded")
            // RFC 7662: Envia o token e as credenciais do cliente
            body = "token=$token" +
                    "&client_id=${configSource.get("AUTH_CLIENT_ID")}" +
                    "&client_secret=${configSource.get("AUTH_CLIENT_SECRET")}"
        }
        return when (val result = httpClient.executeRequest<Map<String, Any>, String>(config)) {
            is HttpCallResult.Success -> {
                val claims = result.data
                // Verificação obrigatória do campo 'active'
                if (claims["active"] == true) {
                    claims
                } else {
                    throw IllegalStateException("Token inativo ou inválido")
                }
            }
            else -> throw IllegalStateException("Falha na introspecção do token")
        }
    }
}

