package org.omniai.sdk.adapters.anthropic

import org.omniai.sdk.core.http.HttpTransportClient

internal actual fun defaultTransportClient(): HttpTransportClient {
    throw UnsupportedOperationException("Default transport client is not available for JS target")
}

