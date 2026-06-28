package org.omniai.sdk.core.http

import org.omniai.sdk.ports.outbound.http.HttpTransportClient

fun defaultHttpTransportClient(): HttpTransportClient = KtorHttpTransportClient.default()
