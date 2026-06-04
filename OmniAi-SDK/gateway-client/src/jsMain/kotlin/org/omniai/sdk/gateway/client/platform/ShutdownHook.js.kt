package org.omniai.sdk.gateway.client.platform

actual fun addShutdownHook(hook: () -> Unit) {
    try {
        js("process.on('SIGINT', function() { hook(); process.exit(); })")
        js("process.on('SIGTERM', function() { hook(); process.exit(); })")
        js("process.on('exit', function() { hook(); })")
    } catch (e: dynamic) {
        // Fallback for browser environments where process doesn't exist
    }
}
