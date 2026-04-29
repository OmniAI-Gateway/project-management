package org.omniai.sdk.auth.domain


import org.omniai.sdk.core.http.HttpTransportClient

sealed interface AuthSetupConfig {

    data object Off : AuthSetupConfig

    data class Discovery(
        val discoveryUrl: String,
        val httpClient: HttpTransportClient,
        val expectedAudience: String,
        val authClientId: String,
        val authClientSecret: String
    ) : AuthSetupConfig
}