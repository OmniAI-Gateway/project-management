package org.omniai.sdk.interceptors.auth

import org.omniai.sdk.core.http.*

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
            is HttpCallResult.Success -> result.data
            else -> TODO("IMPLEMENTAR O RESTO")
        }
    }

    override suspend fun exchangeApiKey(apiKey: String): String {
        val url = tokenEndpoint ?: throw IllegalStateException("Token endpoint não configurado no Discovery")
        val clientId = configSource.get("AUTH_CLIENT_ID") ?: ""
        val clientSecret = configSource.get("AUTH_CLIENT_SECRET") ?: ""

        val config = requestConfig(url) {
            method = HttpMethod.POST
            header("Content-Type", "application/x-www-form-urlencoded")

            body = "grant_type=urn:ietf:params:oauth:grant-type:token-exchange" +
                    "&subject_token=$apiKey" +
                    "&subject_token_type=urn:ietf:params:oauth:token-type:access_token" +
                    "&client_id=$clientId" +
                    "&client_secret=$clientSecret"
        }

        return when (val result = httpClient.executeRequest<TokenExchangeResponse, String>(config)) {
            is HttpCallResult.Success -> result.data.accessToken
            else -> TODO("IMPLEMENTAR O RESTO")
        }
    }
}
