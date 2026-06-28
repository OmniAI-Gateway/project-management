package org.omniai.gateway.app

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

fun buildJsonConfig(): Json =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

fun Application.configureHttp(jsonConfig: Json) {
    install(ContentNegotiation) {
        json(jsonConfig)
    }
}
