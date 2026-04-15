package org.omniai.sdk.interceptors.auth

import org.omniai.sdk.core.http.HttpCallResult
import org.omniai.sdk.core.http.HttpMethod
import org.omniai.sdk.core.http.HttpTransportClient
import org.omniai.sdk.core.http.executeRequest
import org.omniai.sdk.core.http.requestConfig

data class OidcMetadata(
    val issuer: String,
    val jwksUri: String,
    val introspectionEndpoint: String? = null,
    val tokenEndpoint: String? = null 
)

class OidcDiscovery(private val httpClient: HttpTransportClient) {
    suspend fun fetchMetadata(url: String): HttpCallResult<OidcMetadata> {
        val discoveryUrl = when {
            url.contains("/.well-known/") -> url
            else -> "${url.removeSuffix("/")}/.well-known/openid-configuration"
        }
        val config = requestConfig<Unit>(discoveryUrl) {
            method = HttpMethod.GET
        }
        return httpClient.executeRequest<OidcMetadata, Unit>(config)
    }
}