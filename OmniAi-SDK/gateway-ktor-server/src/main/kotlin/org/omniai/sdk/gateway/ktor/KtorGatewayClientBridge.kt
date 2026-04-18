package org.omniai.sdk.gateway.ktor

import io.ktor.server.routing.Routing
import org.omniai.sdk.gateway.client.GatewayConfigDsl

fun GatewayConfigDsl.installKtorNetwork(
    routing: Routing,
    configure: AiGatewayKtorConfigDsl.() -> Unit = {}
) {
    network {
        use(routing.ktorServerAdapter(configure))
    }
}

