package org.omniai.sdk.ports.outbound.http

import org.omniai.sdk.core.http.KtorHttpTransportClient

fun defaultHttpTransportClient(): HttpTransportClient = KtorHttpTransportClient.default()

