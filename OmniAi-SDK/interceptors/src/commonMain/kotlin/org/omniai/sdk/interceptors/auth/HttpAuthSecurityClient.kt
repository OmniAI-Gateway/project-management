package org.omniai.sdk.interceptors.auth

import org.omniai.sdk.core.http.*

expect fun parsePublicKeyFromJson(jwksJson: String, keyId: String?): Any

private data class TokenExchangeResponse(
    val accessToken: String
)

class HttpAuthSecurityClient(
    private val httpClient: HttpTransportClient,
    private val jwksUri: String,
    private val tokenEndpoint: String?,
    private val configSource: ConfigSource
) : AuthSecurityInfrastructure {


    override suspend fun getPublicKey(issuer: String, keyId: String?): Any {
        val config = requestConfig<Unit>(jwksUri) {
            method = HttpMethod.GET
        }

        return when (val result = httpClient.executeRequest<String, Unit>(config)) {
            is HttpCallResult.Success -> {
                parsePublicKeyFromJson(result.data, keyId)
            }
            is HttpCallResult.ApiError -> throw RuntimeException("Erro ao buscar chaves: Status ${result.code}")
            is HttpCallResult.NetworkError -> throw RuntimeException("Erro de rede ao buscar chaves", result.exception)
            else -> throw RuntimeException("Erro desconhecido ao carregar chaves públicas")
        }
    }

    override suspend fun exchangeApiKey(apiKey: String): String {
        val url = tokenEndpoint ?: throw IllegalStateException("Token endpoint não configurado no Discovery")
        
        val config = requestConfig(url) {
            method = HttpMethod.POST
            header("Content-Type", "application/x-www-form-urlencoded")
            body = "grant_type=urn:ietf:params:oauth:grant-type:token-exchange" +
                    "&subject_token=$apiKey" +
                    "&subject_token_type=urn:ietf:params:oauth:token-type:access_token" +
                    "&client_id=${configSource.get("AUTH_CLIENT_ID")}" +
                    "&client_secret=${configSource.get("AUTH_CLIENT_SECRET")}"
        }

        return when (val result = httpClient.executeRequest<TokenExchangeResponse, String>(config)) {
            is HttpCallResult.Success -> result.data.accessToken
            else -> throw IllegalStateException("Falha na troca de API Key por Token")
        }
    }
}
