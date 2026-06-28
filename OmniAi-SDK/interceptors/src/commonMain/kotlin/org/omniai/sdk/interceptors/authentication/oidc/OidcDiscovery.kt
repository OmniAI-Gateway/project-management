package org.omniai.sdk.interceptors.auth.oidc

import org.omniai.sdk.interceptors.auth.domain.OidcMetadata
import org.omniai.sdk.ports.outbound.http.HttpCallResult
import org.omniai.sdk.ports.outbound.http.HttpMethod
import org.omniai.sdk.ports.outbound.http.HttpTransportClient
import org.omniai.sdk.ports.outbound.http.executeRequest
import org.omniai.sdk.ports.outbound.http.requestConfig

class OidcDiscovery(
    private val httpClient: HttpTransportClient,
) {
    suspend fun fetchMetadata(url: String): HttpCallResult<OidcMetadata> {
        val config =
            requestConfig<Unit>(url) {
                method = HttpMethod.GET
            }
        return httpClient.executeRequest<OidcMetadata, Unit>(config)
    }
}
