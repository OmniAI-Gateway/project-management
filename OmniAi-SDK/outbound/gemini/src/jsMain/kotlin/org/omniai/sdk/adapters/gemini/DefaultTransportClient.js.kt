package org.omniai.sdk.adapters.gemini

import org.omniai.sdk.core.http.HttpTransportClient

internal actual fun defaultTransportClient(): HttpTransportClient {
    throw UnsupportedOperationException("Default transport client is not available for JS target")
}

