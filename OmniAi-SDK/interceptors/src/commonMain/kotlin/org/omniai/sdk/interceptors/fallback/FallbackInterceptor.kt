package org.omniai.sdk.interceptors.fallback

import org.omniai.sdk.core.commom.AttributeKey
import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.InterceptorChain
import org.omniai.sdk.core.pipeline.PipelineResult
import org.omniai.sdk.core.ports.OutboundPort

class FallbackInterceptor(
    private val outbounds: List<OutboundPort>,
    private val deniedOutboundsKey: AttributeKey<Set<String>>
) : Interceptor {

    override suspend fun handle(context: GatewayContext, chain: InterceptorChain): PipelineResult {
        var lastResult: PipelineResult? = null
        val attributes = context.attributes

        val initiallyDenied = attributes[deniedOutboundsKey] ?: emptySet()
        val denied = initiallyDenied.toMutableSet()

        val primary = outbounds.find { it.provider.value == context.request.provider.value && it.model.model == context.request.model }
        
        val alternatives = outbounds.filterNot { it === primary }
        
        val sequenceToTry = listOfNotNull(primary) + alternatives

        for (outbound in sequenceToTry) {
            if (denied.contains(outbound.key)) {
                continue
            }

            val newRequest = context.request.copy(
                provider = outbound.provider,
                model = outbound.model.model
            )
            
            attributes[deniedOutboundsKey] = denied.toSet()

            val newContext = GatewayContext(
                request = newRequest,
                attributes = attributes,
                mode = context.mode,
                res = context.res
            )

            // if successful or no result, we return
            when (val result = chain.proceed(newContext)) {
                is PipelineResult.Unary -> return result
                is PipelineResult.Stream -> return result
                is PipelineResult.NoResult -> return result
                is PipelineResult.Error -> {
                    lastResult = result
                    denied.add(outbound.key)
                }
            }
        }

        return lastResult ?: PipelineResult.NoResult
    }
}
