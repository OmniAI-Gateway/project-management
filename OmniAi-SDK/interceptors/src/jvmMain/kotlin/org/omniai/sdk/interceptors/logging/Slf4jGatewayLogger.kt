package org.omniai.sdk.interceptors.logging

import org.omniai.sdk.interceptors.logger.GatewayLogger
import org.slf4j.LoggerFactory

class Slf4jGatewayLogger(
    loggerClass: Class<*>,
) : GatewayLogger {
    private val logger = LoggerFactory.getLogger(loggerClass)

    override fun info(
        message: String,
        vararg args: Any?,
    ) {
        logger.info(message, *args)
    }

    override fun warn(
        message: String,
        vararg args: Any?,
    ) {
        logger.warn(message, *args)
    }

    override fun error(
        message: String,
        vararg args: Any?,
    ) {
        logger.error(message, *args)
    }
}
