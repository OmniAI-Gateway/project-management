package org.omniai.gateway.interceptors

interface GatewayLogger {
    fun info(message: String, vararg args: Any?)
    fun warn(message: String, vararg args: Any?)
    fun error(message: String, vararg args: Any?)
}

object NoOpGatewayLogger : GatewayLogger {
    override fun info(message: String, vararg args: Any?) = Unit
    override fun warn(message: String, vararg args: Any?) = Unit
    override fun error(message: String, vararg args: Any?) = Unit
}

