package org.omniai.sdk.adapters.gemini

import org.omniai.sdk.core.http.HttpTransportClient

internal actual fun defaultTransportClient(): HttpTransportClient = KtorHttpTransportClient.default()

