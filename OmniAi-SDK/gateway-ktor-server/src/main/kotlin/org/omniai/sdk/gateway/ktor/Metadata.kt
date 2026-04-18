package org.omniai.sdk.gateway.ktor

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import org.omniai.sdk.binders.ConfigurableMetadataBinder
import org.omniai.sdk.binders.IncomingContext
import org.omniai.sdk.binders.server.buildServerMetadataBinder
import org.omniai.sdk.core.commom.key
import org.omniai.sdk.domain.common.AUTH_BEARER_TOKEN_KEY

const val CLIENT_IP_METADATA_KEY: String = "gateway.request.clientIp"

val AuthBearerTokenMetadataKey = key<String>(AUTH_BEARER_TOKEN_KEY)
val ClientIpMetadataKey = key<String>(CLIENT_IP_METADATA_KEY)
val GeminiModelMetadataKey = key<String>("gemini.model")

fun defaultKtorRequestMetadataBinder(): ConfigurableMetadataBinder =
    buildServerMetadataBinder {
        header(HttpHeaders.Authorization).bindTo(AuthBearerTokenMetadataKey) { header ->
            header.substringAfter("Bearer ", "").trim().takeIf { it.isNotBlank() }
        }
        property("clientIp") bindTo ClientIpMetadataKey
        path("model") bindTo GeminiModelMetadataKey
    }

internal class KtorIncomingContext(
    private val call: ApplicationCall,
    private val clientIp: String?
) : IncomingContext {
    override fun getHeader(key: String): String? = call.request.headers[key]

    override fun getQueryParam(key: String): String? = call.request.queryParameters[key]

    override fun getPathParam(key: String): String? = call.parameters[key]

    override fun getProperty(key: String): String? = when (key) {
        "clientIp" -> clientIp
        else -> null
    }
}

internal fun ApplicationCall.extractClientIp(): String? {
    val xForwardedFor = request.headers["X-Forwarded-For"]
        ?.substringBefore(',')
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    if (xForwardedFor != null) return xForwardedFor

    val xRealIp = request.headers["X-Real-IP"]
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    if (xRealIp != null) return xRealIp

    return request.local.remoteHost.takeIf { it.isNotBlank() }
}


