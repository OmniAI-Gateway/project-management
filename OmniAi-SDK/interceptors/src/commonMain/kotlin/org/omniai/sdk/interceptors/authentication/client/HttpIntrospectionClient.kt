package org.omniai.sdk.interceptors.auth.client

import org.omniai.sdk.ports.outbound.http.*
import org.omniai.sdk.interceptors.auth.domain.IntrospectionResult
import org.omniai.sdk.interceptors.auth.domain.OPAQUE
import org.omniai.sdk.interceptors.auth.domain.OpaqueToken
import org.omniai.sdk.interceptors.auth.dto.IntrospectionDto
import org.omniai.sdk.interceptors.auth.interfaces.IntrospectionCache
import org.omniai.sdk.interceptors.auth.utils.urlEncode
import kotlin.io.encoding.Base64

class HttpIntrospectionClient(
    private val cache: IntrospectionCache,
    private val endpoint: String,
    private val clientId: String,
    private val clientSecret: String?,
    private val httpClient: HttpTransportClient,
) {
    suspend fun introspect(token: OpaqueToken): IntrospectionResult? {
        val wrapper = OPAQUE(token)

        cache.get(wrapper)?.let { cached ->
            return if (cached.active) cached else null
        }
        val requestCfg = requestConfig(endpoint) {
            method = HttpMethod.POST
            header("Content-Type", "application/x-www-form-urlencoded")
            val credentials = "$clientId:$clientSecret"
            val encoded = Base64.encode(credentials.encodeToByteArray())
            header("Authorization", "Basic $encoded")
            body = "token=${urlEncode(token.token)}"
        }

        val result: IntrospectionResult = when (val res = httpClient.executeRequest<IntrospectionDto, String>(requestCfg)) {
            is HttpCallResult.Success -> res.data.toDomain()
            else -> return null
        }

        cache.put(wrapper, result)

        return if (result.active) result else null
    }
}
