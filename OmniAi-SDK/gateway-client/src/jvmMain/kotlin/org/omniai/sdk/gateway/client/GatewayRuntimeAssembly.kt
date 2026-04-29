package org.omniai.sdk.gateway.client
import org.omniai.gateway.services.PipelineBackedInferenceService
import org.omniai.gateway.services.routingInferenceServiceFactory
import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.core.http.HttpTransportClient
import org.omniai.sdk.core.pipeline.GatewayPipelineBuilder
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.gateway.client.auth.AuthorizationServerConfig
import org.omniai.sdk.inbound.anthropic.AnthropicInboundAdapter
import org.omniai.sdk.inbound.gemini.GeminiInboundAdapter
import org.omniai.sdk.inbound.openai.OpenAiInboundAdapter
import org.omniai.sdk.interceptors.auth.AuthContextInterceptor
import org.omniai.sdk.auth.domain.AuthSetupConfig


suspend fun GatewayDefinition.assemble(
    httpClient: HttpTransportClient
): GatewayRuntime {
    val service = resolveService(httpClient)
    val openAiInbound = if (inbounds.installOpenAi) OpenAiInboundAdapter(service) else null
    val anthropicInbound = if (inbounds.installAnthropic) AnthropicInboundAdapter(service) else null
    val geminiInbound = if (inbounds.installGemini) GeminiInboundAdapter(service) else null
    val customInbounds = inbounds.customFactories.mapValues { (_, factory) -> factory(service) }
    return GatewayRuntime(
        service = service,
        inbounds = GatewayInboundAdapters(
            openAi = openAiInbound,
            anthropic = anthropicInbound,
            gemini = geminiInbound,
            custom = customInbounds
        ),
        metadata = TypedMap()
    )
}
suspend fun GatewayDefinition.start(httpClient: HttpTransportClient): GatewayRuntime {
    val runtime = assemble(httpClient)
    networkAdapters.forEach { adapter ->
        adapter.connect(runtime)
    }
    return runtime
}
private suspend fun GatewayDefinition.resolveService(httpClient: HttpTransportClient): InferenceServicePort {
    return when (val selected = aiServices) {
        is AiServiceSelection.Custom -> selected.service
        AiServiceSelection.BuiltIn -> {
            val terminal = routingInferenceServiceFactory(outboundPorts)
            val interceptorsList = buildInterceptors(httpClient)
            val pipeline = GatewayPipelineBuilder().apply {
                interceptorsList.forEach { install(it) }
                installService(terminal)
            }.build()
            PipelineBackedInferenceService(pipeline)
        }
    }
}
private suspend fun GatewayDefinition.buildInterceptors(httpClient: HttpTransportClient): List<Interceptor> {
    val resolved = mutableListOf<Interceptor>()
    resolved += buildAuthorizationInterceptor(authorizationServer, httpClient)
    if (metrics.enabled) {
        resolved += metrics.interceptors
    }
    resolved += interceptors.global
    interceptors.localByProvider.forEach { (provider, list) ->
        list.forEach { interceptor ->
            resolved += providerScoped(provider, interceptor)
        }
    }
    return resolved
}
private suspend fun buildAuthorizationInterceptor(
    config: AuthorizationServerConfig,
    httpClient: HttpTransportClient
): Interceptor {
    return when (config) {
        AuthorizationServerConfig.None -> AuthContextInterceptor.build(AuthSetupConfig.Off)
        is AuthorizationServerConfig.Custom -> AuthContextInterceptor(config.authenticator, null)
        is AuthorizationServerConfig.Discovery -> {
            AuthContextInterceptor.build(
                AuthSetupConfig.Discovery(
                    discoveryUrl = config.discoveryUrl,
                    httpClient = httpClient,
                    expectedAudience = config.expectedAudience,
                    authClientId = config.clientId ?: "",
                    authClientSecret = config.clientSecret ?: ""
                )
            )
        }
    }
}

private fun providerScoped(provider: Provider, delegate: Interceptor): Interceptor =
    Interceptor { context, chain ->
        if (context.request.provider.value == provider.value) {
            delegate.handle(context, chain)
        } else {
            chain.proceed(context)
        }
    }
