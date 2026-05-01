package org.omniai.sdk.interceptors.logger

object NoOpGatewayLogger : GatewayLogger {
    override fun info(message: String, vararg args: Any?) = Unit
    override fun warn(message: String, vararg args: Any?) = Unit
    override fun error(message: String, vararg args: Any?) = Unit
}

