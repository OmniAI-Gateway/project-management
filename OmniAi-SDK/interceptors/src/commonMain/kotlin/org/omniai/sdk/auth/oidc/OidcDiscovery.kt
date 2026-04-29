package org.omniai.sdk.auth.oidc

import org.omniai.sdk.auth.domain.OidcMetadata
import org.omniai.sdk.core.http.HttpCallResult
import org.omniai.sdk.core.http.HttpMethod
import org.omniai.sdk.core.http.HttpTransportClient
import org.omniai.sdk.core.http.executeRequest
import org.omniai.sdk.core.http.requestConfig

class OidcDiscovery(private val httpClient: HttpTransportClient) {
    suspend fun fetchMetadata(url: String): HttpCallResult<OidcMetadata> {
        val config = requestConfig<Unit>(url) {
            method = HttpMethod.GET
        }
        return httpClient.executeRequest<OidcMetadata, Unit>(config)
    }
}

