package org.omniai.sdk.core.http

class RequestConfigBuilder<T>(private val rawUrl: String) {
    var method: HttpMethod = HttpMethod.GET
    var numberOfTries: Int = 3
    var body: T? = null

    private val headers = mutableMapOf<String, MutableList<String>>()
    private val queryParams = mutableMapOf<String, MutableList<String>>()

    private val pathParams = mutableMapOf<String, String>()

    fun header(key: String, value: String) {
        headers.getOrPut(key) { mutableListOf() }.add(value)
    }

    fun parameter(key: String, value: String) {
        queryParams.getOrPut(key) { mutableListOf() }.add(value)
    }

    fun pathParam(key: String, value: Any) {
        pathParams[key] = value.toString()
    }

    fun build(): RequestConfig<T> {
        val resolvedUrl = resolveUrlTemplate(rawUrl, pathParams)

        return RequestConfig(
            url = resolvedUrl,
            method = method,
            numberOfTries = numberOfTries,
            headers = headers,
            queryParams = queryParams,
            body = body
        )
    }
}
