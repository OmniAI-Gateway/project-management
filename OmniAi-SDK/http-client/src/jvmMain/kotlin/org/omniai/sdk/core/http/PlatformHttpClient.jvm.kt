package org.omniai.sdk.core.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

internal actual fun defaultPlatformHttpClient(json: Json): HttpClient =
    HttpClient(OkHttp) {
        engine {
            config {
                pingInterval(30, TimeUnit.SECONDS)
            }
        }
        installDefaultTransportPlugins(json)
    }
