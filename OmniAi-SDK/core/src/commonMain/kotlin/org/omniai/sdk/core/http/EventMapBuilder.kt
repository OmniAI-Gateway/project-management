package org.omniai.sdk.core.http

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

class EventMapBuilder<E : Any> {
    val map = mutableMapOf<String, KSerializer<out E>>()

    inline fun <reified T : E> on(eventName: String) {
        map[eventName] = serializer<T>()
    }
}