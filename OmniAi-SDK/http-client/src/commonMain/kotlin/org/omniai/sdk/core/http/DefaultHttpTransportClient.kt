package org.omniai.sdk.ports.outbound.http

fun defaultHttpTransportClient(): HttpTransportClient = KtorHttpTransportClient.default()

