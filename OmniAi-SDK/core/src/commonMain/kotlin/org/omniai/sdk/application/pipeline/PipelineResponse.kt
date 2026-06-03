package org.omniai.sdk.application.pipeline

fun gatewayPipeline(block: GatewayPipelineBuilder.() -> Unit): GatewayPipeline {
    return GatewayPipelineBuilder().apply(block).build()
}