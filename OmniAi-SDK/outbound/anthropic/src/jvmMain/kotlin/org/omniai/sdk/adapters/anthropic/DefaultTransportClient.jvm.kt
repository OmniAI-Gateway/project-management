package org.omniai.sdk.adapters.anthropic

import org.omniai.sdk.core.http.HttpTransportClient

internal actual fun defaultTransportClient(): HttpTransportClient = KtorHttpTransportClient.default()

