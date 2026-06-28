package org.omniai.sdk.core.http

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal expect fun defaultPlatformHttpClient(json: Json): HttpClient

internal fun HttpClientConfig<*>.installDefaultTransportPlugins(json: Json) {
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = Long.MAX_VALUE
        requestTimeoutMillis = Long.MAX_VALUE
    }
    install(ContentNegotiation) {
        json(json)
    }
    install(SSE)
}
