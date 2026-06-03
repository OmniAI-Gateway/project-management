package org.omniai.sdk.ports.outbound.http

inline fun <T> requestConfig(
    url: String,
    block: RequestConfigBuilder<T>.() -> Unit = {},
): RequestConfig<T> {
    val builder = RequestConfigBuilder<T>(url)
    builder.block()
    return builder.build()
}
