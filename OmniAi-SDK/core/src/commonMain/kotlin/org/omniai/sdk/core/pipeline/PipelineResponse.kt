package org.omniai.sdk.core.pipeline

fun gatewayPipeline(block: GatewayPipelineBuilder.() -> Unit): GatewayPipeline {
    return GatewayPipelineBuilder().apply(block).build()
}