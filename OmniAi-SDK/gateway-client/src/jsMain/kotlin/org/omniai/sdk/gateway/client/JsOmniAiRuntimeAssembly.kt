package org.omniai.sdk.gateway.client

import org.omniai.sdk.core.http.HttpTransportClient
import org.omniai.sdk.gateway.client.core.OmniAiRuntime
import org.omniai.sdk.gateway.client.dsl.JsOmniAiConfig
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.js.Promise
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

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
        httpClient: dynamic
    ): Promise<dynamic> = GlobalScope.promise {
        // Here we would delegate to the actual multiplatform or JS-specific assembly.
        // For pure JS compatibility, we return a Promise that will resolve with the Runtime.
        // This relies on the core Kotlin `assemble` logic once `services` are fully multiplatform.
        val transportClient = httpClient.unsafeCast<HttpTransportClient>()
        
        // As a placeholder for JS compatibility logic:
        // config.config.assemble(transportClient) 
        // (Assuming a common assemble method becomes available or we implement a JS-specific service factory)
        
        // This is a compatibility layer stub to show the API structure requested.
        js("({ status: 'assembled', config: config })")
    }

    /**
     * Starts the OmniAi Gateway server or execution pipeline in the JS environment.
     */
    @JsName("startServer")
    fun startServer(
        config: JsOmniAiConfig,
        httpClient: dynamic,
        serverLogic: () -> Unit
    ): Promise<Unit> = GlobalScope.promise {
        // Similar to the JVM assembly, we would resolve the services and connect adapters here.
        val transportClient = httpClient.unsafeCast<HttpTransportClient>()
        
        // Initialization logic for JS environment
        val inbounds = config.config.inbounds
        
        // We'd connect the adapters to connectors just like in JVM
        // inbounds.openAiConnector?.let { connector -> ... }
        
        // Execute the user server logic callback
        serverLogic()
    }
}
