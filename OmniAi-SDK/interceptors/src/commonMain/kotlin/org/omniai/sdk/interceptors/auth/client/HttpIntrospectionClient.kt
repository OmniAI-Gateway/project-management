package org.omniai.sdk.interceptors.auth.client

import org.omniai.sdk.core.http.*
import org.omniai.sdk.interceptors.auth.domain.IntrospectionResult
import org.omniai.sdk.interceptors.auth.domain.OPAQUE
import org.omniai.sdk.interceptors.auth.domain.OpaqueToken
import org.omniai.sdk.interceptors.auth.dto.IntrospectionDto
import org.omniai.sdk.interceptors.auth.interfaces.IntrospectionCache
import org.omniai.sdk.interceptors.auth.utils.urlEncode
import kotlin.io.encoding.Base64

/**
 * Handles opaque token introspection (RFC 7662) with integrated caching.
 *
 * Caching behaviour:
 * - **Positive results** (active=true): cached for `positiveCacheTtl` (min with token `exp`).
 * - **Negative results** (active=false): cached for `negativeCacheTtl` to prevent brute-force / DDoS.
 *
 * The raw token is never passed to the cache — [IntrospectionCache] hashes it internally.
 */
class HttpIntrospectionClient(
    private val cache: IntrospectionCache,
    private val endpoint: String,
    private val clientId: String,
    private val clientSecret: String?,
    private val httpClient: HttpTransportClient,
) {

    /**
     * Introspects [token], returning [IntrospectionResult] when active, or `null` when inactive/error.
     */
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
