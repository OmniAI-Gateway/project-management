package org.omniai.sdk.core.http


data class RequestConfig<T>(
    val url: String,
    val method: HttpMethod = HttpMethod.GET,
    val numberOfTries: Int = 3,
    val queryParams: Map<String, List<String>> = emptyMap(),
    val headers: Map<String, List<String>> = emptyMap(),
    val body: T? = null
)
