package org.omniai.gateway.app

import org.omniai.sdk.gateway.client.outbound.OutboundTarget

fun gatewayOutbounds(config: GatewayConfig): List<OutboundTarget> = buildList {
    config.providers.forEach { providerConfig ->
        providerConfig.models.forEach { model ->
            add(
                OutboundTarget(
                    outboundClass = providerConfig.provider.outboundFactory(),
                    model = model,
                    apiKey = providerConfig.apiKey,
                    baseUrl = providerConfig.baseUrl
                )
            )
        }
    }
}

