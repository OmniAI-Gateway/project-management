package org.omniai.sdk.ports.outbound.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json

internal actual fun defaultPlatformHttpClient(json: Json): HttpClient =
    HttpClient(OkHttp) {
        engine {
            config {
                pingInterval(30, TimeUnit.SECONDS)
            }
        }
        installDefaultTransportPlugins(json)
    }

