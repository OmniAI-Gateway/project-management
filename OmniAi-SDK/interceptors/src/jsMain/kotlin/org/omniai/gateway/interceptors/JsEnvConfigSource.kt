package org.omniai.gateway.interceptors

import org.omniai.sdk.interceptors.auth.ConfigSource

object JsEnvConfigSource : ConfigSource {
    @Suppress("UnsafeCastFromDynamic")
    override fun get(name: String): String? {
        val value = js("(typeof process !== 'undefined' && process && process.env) ? process.env[name] : undefined")
        return value as? String
    }
}

