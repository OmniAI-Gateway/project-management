package org.omniai.sdk.logger

interface GatewayLogger {
    fun info(message: String, vararg args: Any?)
    fun warn(message: String, vararg args: Any?)
    fun error(message: String, vararg args: Any?)
}

