package org.omniai.sdk.gateway.client

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import org.omniai.sdk.gateway.client.dsl.JsOmniAiConfig
import org.omniai.sdk.ports.outbound.http.HttpTransportClient
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.js.Promise

@JsExport
@JsName("OmniAiGatewayRuntime")
object JsOmniAiGatewayRuntime {
    /**
     * Assembles the OmniAiRuntime from the JS configuration.
     * Note: This returns a Promise since JS does not have native coroutines.
     *
     * In a full JS implementation, this would build the pipeline logic
     * using JS-compatible HTTP clients and services.
     */
    @JsName("assemble")
    fun assemble(
        config: JsOmniAiConfig,
        httpClient: dynamic,
    ): Promise<dynamic> =
        GlobalScope.promise {
            val transportClient = httpClient.unsafeCast<HttpTransportClient>()
            config.config.assemble(transportClient)
        }

    /**
     * Starts the OmniAi Gateway server or execution pipeline in the JS environment.
     */
    @JsName("startServer")
    fun startServer(
        config: JsOmniAiConfig,
        httpClient: dynamic,
        onStart: () -> Unit,
        onEnd: () -> Unit = {},
    ): Promise<Unit> =
        GlobalScope.promise {
            val transportClient = httpClient.unsafeCast<HttpTransportClient>()
            config.config.startServer(transportClient, onStart, onEnd)
        }
}
