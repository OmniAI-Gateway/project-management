package org.omniai.gateway.interceptors

import kotlin.js.console

class JsConsoleGatewayLogger(
    private val name: String = "gateway"
) : GatewayLogger {

    override fun info(message: String, vararg args: Any?) {
        console.info(format(message, args))
    }

    override fun warn(message: String, vararg args: Any?) {
        console.warn(format(message, args))
    }

    override fun error(message: String, vararg args: Any?) {
        console.error(format(message, args))
    }

    private fun format(message: String, args: Array<out Any?>): String {
        var formatted = message
        args.forEach { arg ->
            val replacement = stringifyArg(arg)
            if (formatted.contains("{}")) {
                formatted = formatted.replaceFirst("{}", replacement)
            } else {
                formatted += " $replacement"
            }
        }
        return "[$name] $formatted"
    }

    private fun stringifyArg(arg: Any?): String = when (arg) {
        null -> "null"
        is Throwable -> arg.stackTraceToString()
        else -> arg.toString()
    }
}

