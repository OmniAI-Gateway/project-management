package org.omniai.sdk.ports.outbound.http

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.serializer

suspend inline fun <reified T, V> HttpTransportClient.executeRequest(
    config: RequestConfig<V>
): HttpCallResult<T> = execute(config, serializer<T>())

inline fun <reified T, V> HttpTransportClient.listenEvents(
    config: RequestConfig<V>,
    eventName: String?
): Flow<HttpCallResult<T>> = listen(config, eventName, serializer<T>())


inline fun <reified E : Any, V> HttpTransportClient.listenEvents(
    config: RequestConfig<V>,
    block: EventMapBuilder<E>.() -> Unit
): Flow<HttpCallResult<E>> {
    val builder = EventMapBuilder<E>()
    builder.block()
    return listenMany(config, builder.map)
}