package org.omniai.sdk.gateway.client

class NetworkDsl {
    internal val adapters = mutableListOf<GatewayNetworkAdapter>()

    fun use(adapter: GatewayNetworkAdapter) {
        adapters += adapter
    }
}

