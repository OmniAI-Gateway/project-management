package org.omniai.sdk.gateway.client.platform

actual fun addShutdownHook(hook: () -> Unit) {
    Runtime.getRuntime().addShutdownHook(Thread {
        hook()
    })
}
