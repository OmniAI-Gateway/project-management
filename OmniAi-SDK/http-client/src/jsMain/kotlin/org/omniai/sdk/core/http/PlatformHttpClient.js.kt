package org.omniai.sdk.core.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import kotlinx.serialization.json.Json

internal actual fun defaultPlatformHttpClient(json: Json): HttpClient =
    HttpClient(Js) {
        installDefaultTransportPlugins(json)
    }

