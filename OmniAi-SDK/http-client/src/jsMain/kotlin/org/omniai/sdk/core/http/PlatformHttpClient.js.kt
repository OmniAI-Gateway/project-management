package org.omniai.sdk.ports.outbound.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import kotlinx.serialization.json.Json
import org.omniai.sdk.core.http.installDefaultTransportPlugins

internal actual fun defaultPlatformHttpClient(json: Json): HttpClient =
    HttpClient(Js) {
        installDefaultTransportPlugins(json)
    }

