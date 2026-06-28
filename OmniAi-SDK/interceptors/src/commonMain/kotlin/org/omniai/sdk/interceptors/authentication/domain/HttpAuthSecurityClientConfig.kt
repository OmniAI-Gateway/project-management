package org.omniai.sdk.interceptors.auth.domain

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class HttpAuthSecurityClientConfig(
    val authClientId: String,
    val authClientSecret: String?,
    val jwksCooldown: Duration = 1.minutes,
    val backgroundJwksRefreshInterval: Duration = 12.hours,
    val minimumTimeToFetchKeys: Duration = 1.minutes,
    val introspectionCacheTtl: Duration = 5.minutes,
    val introspectionNegativeCacheTtl: Duration = 30.seconds,
)
