package org.omniai.gateway.interceptors

import org.omniai.sdk.interceptors.auth.ConfigSource

object JvmEnvConfigSource : ConfigSource {
    override fun get(name: String): String? = System.getenv(name)
}

